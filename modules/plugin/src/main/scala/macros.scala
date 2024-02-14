import com.kubukoz.DebugUtils
import dotty.tools.dotc.ast.Trees.Template

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.quoted.ToExpr
import scala.quoted._

inline def toPhp[A](
  inline a: A
): E = ${ phpImpl('a) }

def phpImpl[A](
  e: Expr[A]
)(
  using q: Quotes
): Expr[E] = Expr(phpImpl0(e))

def phpImpl0[A](
  e: Expr[A]
)(
  using q: Quotes
): E = {

  import quotes.reflect.*

  translate(e.asTerm)
}

def allOwners(
  using q: Quotes
)(
  s: q.reflect.Symbol
): List[q.reflect.Symbol] = {
  import quotes.reflect.*
  if (s.maybeOwner == Symbol.noSymbol)
    Nil
  else
    s.owner :: allOwners(s.owner)
}

extension (
  using q: Quotes
)(
  s: q.reflect.Symbol
) {

  def definedOutsideMacroCall: Boolean = {
    import q.reflect.*
    !s.isOwnedWithin(Symbol.spliceOwner)
  }

  // returns true if `s` has an owner within `scope`
  def isOwnedWithin(
    scope: q.reflect.Symbol
  ): Boolean = allOwners(s).contains(scope)

}

def translate(
  using q: Quotes
)(
  e: q.reflect.Tree
): E = {
  import quotes.reflect.*

  object StringContextApply {
    def unapply(
      t: Tree
    ): Option[
      (
        List[Term],
        List[Term],
      )
    ] =
      t match {
        case Apply(
              Select(
                Apply(
                  Select(Select(Select(Ident("_root_"), "scala"), "StringContext"), "apply"),
                  List(Typed(Repeated(constantParts, _), _)),
                ),
                "s",
              ),
              List(Typed(Repeated(args, _), _)),
            ) =>
          Some((constantParts, args))
        case _ => None
      }
  }

  def function(
    name: Option[String],
    args: List[String],
    body: Tree,
    functionScope: Symbol,
  ): E = {
    val isAnonymous = name.isEmpty

    case class VariableRefs[T](
      globals: Set[T]
    ) {
      def addGlobal(
        ident: T
      ): VariableRefs[T] = copy(globals = globals + ident)

      def transform[U](
        f: Set[T] => Set[U]
      ): VariableRefs[U] = VariableRefs(f(globals))

      def map[U](
        f: T => U
      ): VariableRefs[U] = transform(_.map(f))

      def filter(
        f: T => Boolean
      ): VariableRefs[T] = transform(_.filter(f))

      def filterNot(
        f: T => Boolean
      ): VariableRefs[T] = filter(!f(_))
    }

    object VariableRefs {
      def Empty[T]: VariableRefs[T] = VariableRefs(Set.empty)
    }

    object variableReferences extends TreeAccumulator[VariableRefs[Ident]] {
      override def foldTree(
        x: VariableRefs[Ident],
        tree: Tree,
      )(
        owner: Symbol
      ): VariableRefs[Ident] = {
        def recurse = foldOverTree(x, tree)(owner)

        tree match {
          case ident: Ident if ident.symbol.isValDef =>
            if (tree.symbol.isOwnedWithin(functionScope))
              recurse
            else
              x.addGlobal(ident)
          case _ => recurse
        }
      }
    }

    val externals =
      variableReferences
        .foldOverTree(VariableRefs.Empty, body)(body.symbol)

    val globals =
      externals
        .globals
        .map(_.name)
        .filterNot(Set("println", "_root_", "StdIn"))
        .map[E.VariableIdent](E.VariableIdent(_))
        .toList

    val baseBody = translate(body).ensureBlock.returned
    if (isAnonymous) {
      E.FunctionDef(
        name = name,
        argNames = args,
        useRefs = globals,
        body = baseBody,
        mods = Nil,
      )
    } else
      E.FunctionDef(
        name = name,
        argNames = args,
        useRefs = Nil,
        body =
          if (globals.nonEmpty)
            baseBody.prefixAsBlock(
              E.Globals(globals)
            )
          else
            baseBody,
        mods = Nil,
      )
  }

  e match {
    case Inlined(_, _, e) => translate(e)
    case b @ Block(
          List(d @ DefDef(name, List(TermParamClause(args)), _, Some(body))),
          Closure(Ident(n), _),
        ) if d.symbol.isAnonymousFunction && n == name =>
      function(
        // anon functions can't have names
        name = None,
        args = args.map(_.name),
        body = body,
        d.symbol,
      )
    case DefDef(name, _, _, Some(body)) if body.symbol == Symbols.phpNative => E.Blank
    case DefDef(name, List(TermParamClause(args)), _, Some(body)) =>
      function(Some(name), args.map(_.name), body, e.symbol)
    case DefDef(name, Nil, _, Some(body)) => function(Some(name), Nil, body, e.symbol)
    // when something like a ValDef is used as the only expression in a block
    case Block(List(stat), Literal(UnitConstant())) => E.Unit(translate(stat))
    case Block(stats, expr)                         => E.Block(stats.appended(expr).map(translate))
    case Ident(s) =>
      if (e.symbol.definedOutsideMacroCall)
        report.errorAndAbort(
          s"Cannot refer to symbols not compiled with Scala.php. ${e.symbol.fullName} was defined at " + e
            .symbol
            .pos,
          e.pos,
        )

      if (e.symbol.isValDef)
        if (e.symbol.owner.isClassDef)
          E.Select(E.This, s)
        else
          E.VariableIdent(s)
      else
        E.Ident(s)

    case Apply(
          path,
          List(arg1, Typed(Repeated(args, _), _)),
        ) if path.symbol == Symbols.pathsGet =>
      // As a simplification, we treat paths as `/`-concatenated strings.
      // Yes, it'll break down on Windows. No, we don't care.
      translate(arg1).concat(
        args
          .map(translate)
          .map(E.StringLiteral("/").concat(_))
          .foldLeft(E.StringLiteral(""))(_.concat(_))
      )

    case Apply(Select(e1, op @ ("+" | "-" | "*" | "/")), List(e2)) =>
      if (op == "+" && e1.tpe <:< TypeRepr.of[String])
        translate(e1).concat(translate(e2))
      else if (e1.tpe <:< TypeRepr.of[Int])
        E.BinOp(
          translate(e1),
          op,
          translate(e2),
        )
      else
        report.errorAndAbort("couldn't concat values of type " + e1.tpe)

    case StringContextApply(constantParts, args) =>
      val parts = constantParts
        .map { case Literal(StringConstant(s)) => s }
        .map(StringContext.processEscapes(_))
        .map(E.StringLiteral(_))

      val pieces = parts.head :: args.map(translate(_)).zip(parts.tail).flatMap(_.toList)

      pieces
        .filterNot {
          _ == E.StringLiteral("")
        }
        .reduceLeft(_ concat _)

    case Typed(e, _) => translate(e)
    case Assign(lhs, rhs) =>
      E.Unit(
        E.Assign(translate(lhs), translate(rhs))
      )
    case Literal(UnitConstant())     => E.Unit(E.Blank)
    case Literal(StringConstant(v))  => E.StringLiteral(v)
    case Literal(IntConstant(v))     => E.IntLiteral(v)
    case Literal(BooleanConstant(v)) => E.BooleanLiteral(v)
    case ValDef(name, _, Some(v))    => E.Assign(E.VariableIdent(name), translate(v))
    case If(cond, thenp, elsep) =>
      E.If(
        cond = translate(cond),
        thenp = translate(thenp),
        elsep = translate(elsep),
      )
    case Select(Ident("StdIn"), "readLine")    => E.Builtin("readline")
    case Select(New(TypeIdent(tpe)), "<init>") => E.New(tpe)
    case Select(a, "apply")                    => translate(a)
    case Select(_, _) if e.symbol == Symbols.filesReadString =>
      E.Builtin("java_nio_file_Files_readString")
    case s @ Select(a, name) =>
      val base = E.Select(translate(a), name)
      if (
        s.symbol.isDefDef
      ) // Q: why does this work? If the thing is a function with parameters, wouldn't this aply too, and result in some kind of double-application?
        E.Apply(base, List())
      else
        base
    case Apply(Ident("println"), Nil) => E.Echo(E.StringLiteral("\\n"))
    case Apply(Ident("println"), List(msg)) =>
      E.Echo(
        translate(msg)
          .unblockOr(
            report.errorAndAbort(
              "Block parameters aren't supported: " + msg.structure,
              msg.pos,
            )
          )
          .concat(E.StringLiteral("\\n"))
      )

    case Apply(f, List(arg)) if f.symbol == Symbols.arrayApply =>
      E.ArrayLookup(translate(f), translate(arg))
    case Apply(f, args) =>
      E.Apply(
        translate(f),
        args.map(arg =>
          translate(arg).unblockOr(
            report.errorAndAbort(
              "Block parameters aren't supported: " + arg.structure,
              arg.pos,
            )
          )
        ),
      )
    case c @ ClassDef(name, constr, parents, selfOpt, body) =>
      val fields = c.symbol.caseFields.map { f =>
        Field(
          name = f.name,
          mods =
            f.flags.is(Flags.Private) match {
              case true  => Mod.Private :: Nil
              case false => Mod.Public :: Nil
            },
        )
      }
      val methods = body
        .filterNot(_.symbol.flags.is(Flags.Synthetic))
        .filterNot(_.symbol.flags.is(Flags.CaseAccessor))
        .filter(_.symbol.isDefDef)
        .map(translate)

      E.Class(name, fields, methods)
    case NamedArg(_, arg) => translate(arg)
    case other =>
      report.errorAndAbort(s"Unsupported code (${other.show}): " + other.structure, other.pos)
  }

}

private object Symbols {

  def pathsGet(
    using q: Quotes
  ): q.reflect.Symbol = {
    import q.reflect._

    val repeatedStringTypeRepr: TypeRepr = Symbol
      .requiredClass("scala.<repeated>")
      .typeRef
      .appliedTo(TypeRepr.of[String])

    def matchesSignature(
      m: Symbol
    ): Boolean =
      m.paramSymss match {
        case args :: Nil =>
          args.size == 2 &&
          args.map(_.tree).match {
            case ValDef(_, arg1Type, _) :: ValDef(_, arg2Type, _) :: Nil =>
              arg1Type.tpe <:< TypeRepr.of[String] &&
              arg2Type.tpe <:< repeatedStringTypeRepr
            case _ => false
          }
        case _ => false
      }

    Symbol
      .requiredClass("java.nio.file.Paths")
      .companionModule
      .methodMember("get")
      .find(matchesSignature)
      .getOrElse(sys.error("couldn't find matching `Paths.get` method"))
  }

  def filesReadString(
    using q: Quotes
  ): q.reflect.Symbol = {
    import q.reflect._

    def matchesSignature(
      m: Symbol
    ): Boolean =
      m.paramSymss match {
        case List(List(arg)) =>
          arg.tree match {
            case ValDef(_, argType, _) => argType.tpe <:< TypeRepr.of[Path]
            case _                     => false
          }
        case _ => false
      }

    Symbol
      .requiredModule("java.nio.file.Files")
      .methodMember("readString")
      .find(matchesSignature)
      .getOrElse(sys.error("couldn't find matching `Files.readString` method"))
  }

  def arrayApply(
    using q: Quotes
  ): q.reflect.Symbol = {
    import q.reflect._

    Symbol.requiredClass("scala.Array").methodMember("apply").head
  }

  def phpNative(
    using q: Quotes
  ): q.reflect.Symbol = {
    import q.reflect.*

    Symbol.requiredMethod("org.scalaphp.php.native")
  }

}

extension (
  using q: Quotes
)(
  tree: q.reflect.Tree
) {

  def structure = {
    import q.reflect.*
    tree.show(
      using Printer.TreeStructure
    )
  }

}

case class Field(
  name: String,
  mods: List[Mod],
)

enum Mod {
  case Private
  case Public
}

enum E {

  case Class(
    name: String,
    fields: List[Field],
    methods: List[E],
  )

  case New(
    name: String
  )

  case Select(
    expr: E,
    name: String,
  )

  case This

  case Builtin(
    name: String
  )

  case Unit(
    e: E
  )

  case Blank

  case BooleanLiteral(
    value: Boolean
  )

  case StringLiteral(
    value: String
  )

  case IntLiteral(
    value: Int
  )

  case Return(
    e: E
  )

  case Block(
    stats: List[E]
  )

  case BinOp(
    left: E,
    op: String,
    right: E,
  )

  case Globals(
    names: List[VariableIdent]
  )

  case If(
    cond: E,
    thenp: E,
    elsep: E,
  )

  case Echo(
    arg: E
  )

  case VariableIdent(
    name: String
  )

  case Ident(
    name: String
  )

  case Assign(
    lhs: E,
    rhs: E,
  )

  case FunctionDef(
    name: Option[String],
    argNames: List[String],
    useRefs: List[VariableIdent],
    body: E,
    mods: List[Mod],
  )

  case Apply(
    f: E,
    args: List[E],
  )

  case ArrayLookup(
    array: E,
    index: E,
  )

  def concat(
    right: E
  ): E = BinOp(this, ".", right)

  def ensureBlock: E.Block =
    this match {
      case b: Block => b
      case _        => Block(this :: Nil)
    }

  def unblockOr(
    fallback: => E
  ): E =
    this match {
      case Block(List(stat)) => stat
      case Block(_)          => fallback
      case other             => other
    }

  def returned: E =
    this match {
      case If(cond, thenp, elsep) => If(cond, thenp.returned, elsep.returned)
      case Block(stats) =>
        stats.lastOption match {
          case Some(last) => Block(stats.init.appended(last.returned))
          case None       => Block(stats)
        }
      case _: Echo => this
      case other   => Return(other)
    }

  def prefixAsBlock(
    another: E
  ): Block =
    this match {
      case Block(stats) => Block(another :: stats)
      case other        => Block(another :: other :: Nil)
    }

}

def escape(
  name: String
) = name.replace("$", "DOLLAR")

given ToExpr[E] with {

  def apply(
    x: E
  )(
    using Quotes
  ): Expr[E] =
    x match {

      case E.New(name)             => '{ E.New(${ Expr(name) }) }
      case E.Select(expr, name)    => '{ E.Select(${ apply(expr) }, ${ Expr(name) }) }
      case E.Unit(e)               => '{ E.Unit(${ apply(e) }) }
      case E.Blank                 => '{ E.Blank }
      case E.Builtin(name)         => '{ E.Builtin(${ Expr(name) }) }
      case E.BooleanLiteral(value) => '{ E.BooleanLiteral(${ Expr(value) }) }
      case E.StringLiteral(value)  => '{ E.StringLiteral(${ Expr(value) }) }
      case E.IntLiteral(value)     => '{ E.IntLiteral(${ Expr(value) }) }
      case E.Return(e)             => '{ E.Return(${ apply(e) }) }
      case E.Block(stats)          => '{ E.Block(${ Expr.ofList(stats.map(apply)) }) }
      case E.BinOp(left, op, right) =>
        '{ E.BinOp(${ apply(left) }, ${ Expr(op) }, ${ apply(right) }) }
      case E.If(cond, thenp, elsep) =>
        '{ E.If(${ apply(cond) }, ${ apply(thenp) }, ${ apply(elsep) }) }
      case E.Echo(arg)           => '{ E.Echo(${ apply(arg) }) }
      case E.VariableIdent(name) => '{ E.VariableIdent(${ Expr(name) }) }
      case E.Ident(name)         => '{ E.Ident(${ Expr(name) }) }
      case E.Assign(lhs, rhs)    => '{ E.Assign(${ apply(lhs) }, ${ apply(rhs) }) }
      case E.Globals(names) =>
        '{
          E.Globals(${
            Expr.ofList(names.map { vi =>
              '{ E.VariableIdent(${ Expr(vi.name) }) }
            })
          })
        }
      case E.FunctionDef(name, argNames, useRefs, body, mods) =>
        '{
          E.FunctionDef(
            ${ Expr(name) },
            ${ Expr.ofList(argNames.map(Expr(_))) },
            ${
              Expr.ofList(useRefs.map { vi =>
                '{ E.VariableIdent(${ Expr(vi.name) }) }
              })
            },
            ${ apply(body) },
            ${ Expr.ofList(mods.map(Expr(_))) },
          )
        }
      case E.Apply(f, args) => '{ E.Apply(${ apply(f) }, ${ Expr.ofList(args.map(apply)) }) }
      case E.Class(name, fields, body) =>
        '{
          E.Class(
            ${ Expr(name) },
            ${ Expr.ofList(fields.map(Expr(_))) },
            ${ Expr.ofList(body.map(apply)) },
          )
        }
      case E.This                      => '{ E.This }
      case E.ArrayLookup(array, index) => '{ E.ArrayLookup(${ apply(array) }, ${ apply(index) }) }
    }

}

given ToExpr[Mod] with {

  override def apply(
    x: Mod
  )(
    using Quotes
  ): Expr[Mod] =
    x match {
      case Mod.Private => '{ Mod.Private }
      case Mod.Public  => '{ Mod.Public }
    }

}

given ToExpr[Field] with {

  override def apply(
    x: Field
  )(
    using Quotes
  ): Expr[Field] = '{ Field(${ Expr(x.name) }, ${ Expr.ofList(x.mods.map(Expr(_))) }) }

}

private def renderStat(
  e: E
): String =
  e match {
    case _: E.FunctionDef | E.Blank | _: E.If | _: E.Block => render(e)
    case _                                                 => render(e) + ";"
  }

def renderPublic(
  e: E,
  includePrelude: Boolean = true,
): String =
  if (includePrelude)
    renderStdlib + render(e, topLevel = true)
  else
    render(e, topLevel = true)

private val T_PAAMAYIM_NEKUDOTAYIM = "::"

private def renderStdlib =
  s"""|//
      |// scala.php stdlib start
      |//
      |class scala_Unit implements Stringable{
      |  public function __toString() {
      |    return "()";
      |  }
      |
      |  public static function consume() {
      |    return self${T_PAAMAYIM_NEKUDOTAYIM}getInstance();
      |  }
      |
      |  private static $$instance = null;
      |  public static function getInstance() {
      |    if (self${T_PAAMAYIM_NEKUDOTAYIM}$$instance === null) {
      |      self${T_PAAMAYIM_NEKUDOTAYIM}$$instance = new self();
      |    }
      |    return self${T_PAAMAYIM_NEKUDOTAYIM}$$instance;
      |  }
      |}
      |
      |function java_nio_file_Files_readString($$path) {
      |  $$str = file_get_contents($$path);
      |  if ($$str === false) {
      |    throw new Exception("Failed to read file: " . $$path);
      |  }
      |  return $$str;
      |}
      |
      |//
      |// scala.php stdlib end
      |//
      |
      |""".stripMargin

private def render(
  e: E,
  topLevel: Boolean = false,
): String =
  e match {
    case E.Blank                 => ""
    case E.This                  => "$this"
    case E.Unit(expr)            => s"scala_Unit${T_PAAMAYIM_NEKUDOTAYIM}consume(${render(expr)})"
    case E.Builtin(name)         => name
    case E.Return(e)             => s"return ${render(e)}"
    case E.Select(expr, name)    => s"${render(expr)}->$name"
    case E.IntLiteral(value)     => value.toString
    case E.StringLiteral(value)  => '"' + value + '"'
    case E.BooleanLiteral(value) => value.toString()
    case E.Block(stats) =>
      val bodyRendered = stats
        .map(renderStat)
        .mkString("\n")

      if (topLevel)
        bodyRendered
      else
        s"""|{
            |${bodyRendered.indentTrim(2)}
            |}""".stripMargin
    case E.Assign(lhs, rhs)       => render(lhs) + " = " + render(rhs)
    case E.VariableIdent(name)    => s"$$${escape(name)}"
    case E.Ident(name)            => name
    case E.Echo(arg)              => "echo " + render(arg)
    case E.BinOp(left, op, right) => s"${render(left)} $op ${render(right)}"
    case E.Globals(names) =>
      if (names.isEmpty)
        ""
      else
        "global " + names.map(render(_)).mkString(", ")

    case E.FunctionDef(name, argNames, useRefs, body, mods) =>
      val modsString =
        if (mods.isEmpty)
          ""
        else
          mods
            .map {
              case Mod.Private => "private"
              case Mod.Public  => "public"
            }
            .mkString(" ") + " "

      val nameText = name.getOrElse("")

      val paramString = argNames.map(E.VariableIdent(_)).map(render(_)).mkString(", ")

      val useText =
        if (useRefs.isEmpty)
          ""
        else
          " use (" + useRefs.map("&" + render(_)).mkString(", ") + ")"

      val bodyString = render(body)

      s"""|${modsString}function $nameText(${paramString})$useText $bodyString""".stripMargin
    case E.Apply(f, args) => s"${render(f)}(${args.map(render(_)).mkString(", ")})"
    case E.If(cond, thenp, elsep) =>
      s"if (${render(cond)}) ${renderStat(thenp)} else ${renderStat(elsep)}"

    case E.Class(name, fields, methods) =>
      val fieldsString = fields
        .map { f =>
          val modsString =
            if (f.mods.isEmpty)
              ""
            else
              f.mods
                .map {
                  case Mod.Private => "private"
                  case Mod.Public  => "public"
                }
                .mkString(" ") + " "

          s"""$modsString$$${f.name};"""
        }
        .mkString("\n")

      val constructorString = {
        val fieldSetString = fields
          .map { f =>
            s"""$$this->${f.name} = $$${f.name};"""
          }
          .mkString("\n")

        s"""function __construct(${fields.map(_.name.prepended('$')).mkString(", ")}) {
           |${fieldSetString.indentTrim(2)}
           |}""".stripMargin
      }

      val methodsString = methods.map(renderStat(_)).mkString("\n")

      s"""|class ${escape(name)} {
          |${fieldsString.indentTrim(2)}
          |${constructorString.indentTrim(2)}
          |${methodsString.indentTrim(2)}
          |}""".stripMargin
    case E.New(name)                 => s"new ${escape(name)}"
    case E.ArrayLookup(array, index) => s"${render(array)}[${render(index)}]"
  }

extension (
  s: String
) {

  def indentTrim(
    chars: Int
  ): String = s
    .linesIterator
    .map { l =>
      if (l.isBlank)
        l
      else
        " " * chars + l
    }
    .mkString("\n")

}

inline def log[A](
  inline a: A
): A = ${ logImpl('a) }

def logImpl[A](
  e: Expr[A]
)(
  using q: Quotes
): Expr[A] = {
  import quotes.reflect.*

  report.info(
    e.show + ": " +
      e.asTerm.structure,
    e.asTerm.pos,
  )

  e
}
