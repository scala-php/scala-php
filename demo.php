<?php
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
  echo 50 + 20 * 100 / 2 . "\n";
  echo foo(bar($y)) . "\n";
}
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
echo $test = 40 . "\n";
greet("Kuba");
