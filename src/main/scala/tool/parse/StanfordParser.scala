package edu.washington.cs.knowitall
package tool
package parse

import graph._
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import collection._
import java.io.ObjectInputStream
import java.util.Arrays
import edu.stanford.nlp.trees.PennTreebankLanguagePack
import collection.JavaConversions._
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.CoreLabel
import scala.io.Source

object StanfordParser extends DependencyParserMain {
  lazy val parser = new StanfordParser
}

abstract class BaseStanfordParser extends DependencyParser {
  def dependencies(string: String): Iterable[Dependency] = dependencies(string, true)
  def dependencies(string: String, post: Boolean): Iterable[Dependency]
  
  def convertDependency(nodes: Map[Int, DependencyNode], dep: edu.stanford.nlp.trees.TypedDependency) = {
    new Dependency(nodes(dep.gov.index - 1), nodes(dep.dep.index - 1), dep.reln.toString)
  }
  def convertDependencies(nodes: Map[Int, DependencyNode], dep: Iterable[edu.stanford.nlp.trees.TypedDependency]) = {
    // filter out the dependency from the root
    dep.filter(_.gov.index > 0).map(d => convertDependency(nodes, d))
  }
}

class StanfordParser(lp : LexicalizedParser) extends BaseStanfordParser with ConstituencyParser {
  def this() = this(new LexicalizedParser("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"))
  private val tlp = new PennTreebankLanguagePack();
  private val gsf = tlp.grammaticalStructureFactory();

  override def dependencies(string: String, post: Boolean): Iterable[Dependency] = {
    val tree = lp.apply(string)
    val nodes = tree.taggedYield().view.zipWithIndex.map {
      case (tw, i) => (i, new DependencyNode(tw.word, tw.tag, i)) 
    }.toMap
    
    if (post) convertDependencies(nodes, gsf.newGrammaticalStructure(tree).typedDependenciesCCprocessed)
    else convertDependencies(nodes, gsf.newGrammaticalStructure(tree).typedDependencies)
  }

  override def parse(string: String) = {
    var index = 0
    def convertTree(tree: edu.stanford.nlp.trees.Tree): ParseTree = {
      val curindex = index
      index += 1
      val children = tree.children.map(child => convertTree(child))
      if (tree.isPhrasal)
        new ParseTreePhrase(tree.value, curindex, children)
      else if (tree.isPreTerminal)
        new ParseTreePostag(tree.value, curindex, children)
      else
        new ParseTreeToken(tree.value, curindex, children)
    }
    convertTree(lp.apply(string))
  }
}

object StanfordConstituencyParser 
extends ConstituencyParserMain {
  lazy val parser = new StanfordParser();
}