import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Flags.Module
import dotty.tools.dotc.core.Flags.Package
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.plugins.StandardPlugin
import dotty.tools.dotc.quoted.MacroExpansion
import dotty.tools.dotc.report
import dotty.tools.dotc.semanticdb.UnitConstant
import dotty.tools.dotc.typer.TyperPhase
import dotty.tools.dotc.util.NoSourcePosition
import tpd.*

import java.nio.file.Files
import scala.annotation.tailrec
import scala.quoted.Expr
import scala.quoted.Quotes
import scala.quoted.runtime.impl.ExprImpl
import scala.quoted.runtime.impl.NoScope
import scala.quoted.runtime.impl.QuotesImpl
import scala.quoted.runtime.impl.Scope
import scala.quoted.runtime.impl.SpliceScope

final class ScalaPhpCompilerPlugin extends StandardPlugin {
  override val name: String = "scala-php"
  override val description: String = "todo"

  override def init(
    options: List[String]
  ): List[PluginPhase] = List(new ScalaPhpPluginPhase)

}

final class ScalaPhpPluginPhase extends PluginPhase {

  override val phaseName: String = "scala-php-phase"
  override val runsAfter: Set[String] = Set(TyperPhase.name)

  override def transformTemplate(
    t: Template
  )(
    using ctx: Context
  ): Tree = {

    val firstMain = t.body.collectFirst {
      case d: DefDef if d.name.show == "main" => d.rhs
    }

    given Quotes =
      QuotesImpl()(
        using SpliceScope
          .contextWithNewSpliceScope(t.symbol.sourcePos)(
            using MacroExpansion.context(t)
          )
          .withOwner(t.symbol.owner)
      )

    val qq = summon[Quotes]
    import scala.quoted.quotes
    import qq.reflect.*

    firstMain match {
      case Some(main) =>
        val p = java.nio.file.Paths.get("demo.php").toAbsolutePath
        Files.writeString(
          p,
          "<?php\n" + renderPublic(
            phpImpl0(
              ExprImpl(main, NoScope)
            )
          ),
        )
        println(s"Written to $p")

      case None => println("no main found")
    }

    t
  }

}
