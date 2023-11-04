<?php
$x = 42;

function greet($s) {
  global $x;
  echo "hello, " . $s . " " . $x . "\n";
}

greet("Kuba");
