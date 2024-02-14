---
title: "Interoperability with PHP"
sidebar_position: 3
---

Currently, Scala.php allows running native PHP code from Scala, but not the other way around.

In order to call a PHP-defined function, you have to know its signature. For example, [`explode`][explode-docs]:

```php
explode(string $separator, string $string, int $limit = PHP_INT_MAX): array
```

For the example's sake, let's assume we're only interested in providing the first two parameters.

The Scala signature you have to define would be:

```scala
import org.scalaphp.php

@php.native
def explode(
  separator: String,
  string: String,
): Array[String] = php.native
```

:::warning
Currently, [PHP 8's named arguments][php-8-named-args] aren't supported. Scala's named parameters still work - just make sure to match the order of arguments when defining the signature!
:::

Having defined the signature, we can call the function:

```scala
explode(string = "foo bar", separator = " ") // Array("foo", "bar")
```

[php-8-named-args]: https://www.php.net/manual/en/functions.arguments.php#functions.named-arguments
[explode-docs]: https://www.php.net/manual/en/function.explode
