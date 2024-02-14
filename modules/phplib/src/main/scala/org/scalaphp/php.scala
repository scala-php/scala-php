package org.scalaphp

object php {

  final class native extends scala.annotation.StaticAnnotation

  def native: Nothing = intrinsic

  private def intrinsic: Nothing =
    throw new UnsupportedOperationException("intrinsic call wasn't rewritten by the compiler!")
}
