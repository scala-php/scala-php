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
echo bar(420) . "\n";
greet("Kuba");
