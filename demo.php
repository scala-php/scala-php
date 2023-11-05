<?php
//
// scala.php stdlib start
//
class scala_Unit implements Stringable{
  public function __toString() {
    return "()";
  }

  public static function consume() {
    return self::getInstance();
  }

  private static $instance = null;
  public static function getInstance() {
    if (self::$instance === null) {
      self::$instance = new self();
    }
    return self::$instance;
  }
}

//
// scala.php stdlib end
//

$x = 42;
function foo($i) {
  return $i + 1;
}
function bar($i) {
  global $x;
  return foo($x + $i - 1);
}
function greet($s) {
  global $x;
  $y = 50;
  echo $x . "\n";
  echo "hello, " . $s . " " . $x . " " . $y . "\n";
  echo foo(bar($y)) . "\n";
}
echo 50 + 20 * 100 / 2 . "\n";
function fun($b, $b2) {
  if ($b) {
    $z = "hello";
    return $z;
  } else if ($b2) return "goodbye"; else return "secret third option";
}
echo bar(420) . "\n";
greet("Kuba");
echo fun(true, false) . "\n";
echo fun(false, true) . "\n";
echo fun(false, false) . "\n";
$test = 40;
scala_Unit::consume($test = 42);
echo scala_Unit::consume($test = 520) . "\n";
echo scala_Unit::consume($test2 = 520) . "\n";
echo "Demo" . "\n";
echo "Kuba" . "\n";
greet("Kuba");
$comma = "";
$concatDupe = function ($s) use (&$comma) {
  return $s . $comma . $s;
};
scala_Unit::consume($comma = ", ");
echo $concatDupe("hello") . "\n";
echo "What's your name?" . "\n";
$semi = function ($_DOLLAR1) {
  return $_DOLLAR1 . ";";
};
echo $semi("hello") . "\n";
$add = function ($_DOLLAR2, $_DOLLAR3) {
  return $_DOLLAR2 + $_DOLLAR3;
};
echo $add(1, 20) . "\n";
$addCurried = function ($a) {
  return function ($b) use (&$a) {
    return $a + $b;
  };
};
echo $addCurried(1)(20) . "\n";
$addCurriedMultiArgs = function ($a, $b) {
  return function ($c) use (&$a, &$b) {
    return $a + $b + $c;
  };
};
$p1 = $addCurriedMultiArgs;
$p2 = function ($_DOLLAR4) use (&$p1) {
  return $p1(1, $_DOLLAR4);
};
$p3 = $p2(2);
$p4 = $p3(3);
echo $p4 . "\n";
