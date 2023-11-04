<?php
$greeting = "Hello";
$name = "Kuba";

function greet($s) {
  global $greeting;
  $a = 42;
  return $greeting . ", " . $s . " " . $a . "!";
}

echo "Initializing..." . "\n";
echo "\n";
echo greet($name) . "\n";
$name = "Test";
echo greet($name) . "\n";
