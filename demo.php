<?php
$x = 42;

function greet($s) {
  global $x;
  $y = 50;
  echo "hello, " . $s . " " . $x . " " . $y . "\n";
  echo 50 + 20 * 100 / 2 . "\n";
}

greet("Kuba");
