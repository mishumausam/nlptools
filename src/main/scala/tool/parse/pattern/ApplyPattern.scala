package edu.washington.cs.knowitall
package tool
package parse
package pattern

import graph._

import scala.io.Source
import scopt.OptionParser

object ApplyPattern {
  def main(args: Array[String]) {
    val parser = new OptionParser("applypat") {
      var patternFilePath: String = null
      var sentenceFilePath: String = null
      opt("p", "patterns", "<file>", "pattern file", { v: String => patternFilePath = v })
      opt("s", "sentences", "<file>", "sentence file", { v: String => sentenceFilePath = v })
    }

    if (parser.parse(args)) {
      val patternSource = Source.fromFile(parser.patternFilePath)
      val patterns = patternSource.getLines.map(Pattern.deserialize(_)).toList
      patternSource.close

      val sentenceSource = Source.fromFile(parser.sentenceFilePath)
      try {
        for (p <- patterns) {
          println("pattern: " + p)
          for (line <- sentenceSource.getLines) {
            val Array(text, deps) = line.split("\t")
            val graph = new DependencyGraph(text, Dependencies.deserialize(deps))
            for (m <- p(graph)) {
              println(m)
            }
          }
          
          println()
          println()
        }
      } finally {
        sentenceSource.close
      }
    }
  }
}