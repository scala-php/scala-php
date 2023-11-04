<?php
$x = 42;
function foo($i) {
  $i + 1
}
function bar($i) {
  $i - 1
}
function greet($s) {
  global $x, $bar, $foo;
  $y = 50;
  echo "hello, " . $s . " " . $x . " " . $y . "\n";
  echo 50 + 20 * 100 / 2 . "\n";
  echo foo(bar($y)) . "\n";
}
greet("Kuba");
