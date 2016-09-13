package vegas.render

import vegas.DSL.SpecBuilder
import vegas.spec.Spec.ExtendedUnitSpec

/**
  * @author Wen Li.
  */
case class FlatStaticHTMLRenderer(spec: ExtendedUnitSpec) extends BaseHTMLRenderer {

  /**
    * Mixing the script with its dependencies
    * @param script the script to mix in
    * @param additionalImports any additional libs for the script
    * @return
    */
  def requireJSSnippet(script: String, additionalImports: Map[String, String] = Map.empty) =
    s"""
       | (function(){
       |   require.config({
       |     paths: {
       |       "d3": "https://d3js.org/d3.v3.min",
       |       "vg": "https://vega.github.io/vega/vega",
       |       "vl": "https://vega.github.io/vega-lite/vega-lite",
       |       "vg_embed": "https://vega.github.io/vega-editor/vendor/vega-embed"
       |     },
       |     shim: {
       |       vg_embed: {
       |         deps: ["vegas"],
       |         exports: "vg.embed"
       |       },
       |       vg: {
       |         deps: ["d3"],
       |         exports: "vg"
       |       }
       |     }
       |   });
       |   define("vegas", ["d3", "vg", "vl"], function(d3, vg, vl){
       |     window["vg"] = vg;
       |     window["vl"] = vl;
       |     return vg;
       |   });
       |   require([${(additionalImports.values ++ Seq("vg_embed")).map { s => s"'${s}'" }.mkString(", ")}],
       |     function(${(additionalImports.keys ++ Seq("vg_embed")).map { s => s"${s}" }.mkString(", ")} ){
       |     window.vg.embed = vg_embed;
       |     $script
       |   });
       | })();
     """.stripMargin

  def JS(name: String = this.defaultName) = requireJSSnippet(
    s"""
       | var embedSpec = {
       |   mode: "vega-lite",
       |   spec: ${vegas.spec.toJson(spec)}
       | }
       | window.vg.embed("#$name", embedSpec, function(error, result) {
       |   if (error) {
       |     throw(error);
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
  override def show(implicit fn: (String, String) => Unit) = {
    val name = defaultName
    fn(HTML(name), JS(name))
  }

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