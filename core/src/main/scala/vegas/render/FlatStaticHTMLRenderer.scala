package vegas.render

import vegas.DSL.SpecBuilder
import vegas.spec.Spec.ExtendedUnitSpec

/**
  * @author Aish Fenton.
  */
case class FlatStaticHTMLRenderer(spec: ExtendedUnitSpec) extends BaseHTMLRenderer {

  def requireJSSnippet(script: String, additionalImports: Map[String, String] = Map.empty) =
    s"""
       | require([${(JSImports ++ additionalImports).values.map { s => s"'${s}'" }.mkString(", ")}],
       |   function(${(JSImports ++ additionalImports).values.mkString(", ")}) {
       |     ${script}
       | });
     """.stripMargin

  def JS(name: String = this.defaultName) = requireJSSnippet(
    s"""
       | var embedSpec = {
       |   mode: "vega-lite",
       |   spec: ${vegas.spec.toJson(spec)}
       | }
       | vg.embed("#$name", embedSpec, function(error, result) {
       |   if (err.requireType !== 'scripterror') {
       |     throw(err);
       |   }
       | });
    """.stripMargin)

  /**
    * Typically you'll want to use this method to render your chart. Returns a snippet of HTML that will be insert into
    * Jupyter notebooks.
    * The flat style will avoid iframe hassles
    * @param name The name of the chart to use as an HTML id. Defaults to a UUID.
    * @return HTML containing iFrame for embedding
    */
  def HTML(name: String = defaultName) = {
    s"""
       | <div id="${name}"></div>
    """.stripMargin
    }

  // Continence method
  override def show(implicit fn: (String, String) => Unit) = fn(HTML(), JS())

  override def show(implicit fn: (String) => Unit): Unit = {
    throw new IllegalArgumentException(
      """
        | Vegas FlatStaticHTMLRenderer require a display function of type (String, String) => Unit
        | try "implicit val display: (String, String) => Unit = { display.html(_); display.javascript(_) }
      """.stripMargin)
  }
}

object FlatStaticHTMLRenderer {

  implicit def toFlatStaticHTMLRenderer(sb: SpecBuilder): FlatStaticHTMLRenderer = { new FlatStaticHTMLRenderer(sb.spec) }

}