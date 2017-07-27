package sbt
package hoarder

import internal.inc.Hash
import sbt.io.{ Hash => IOHash }

object StampBridge {
  def hashFileContent(s: String) =
    Hash.unsafeFromString(IOHash.toHex(s.getBytes))

}
