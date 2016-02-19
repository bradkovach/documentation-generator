import java.io.{File, BufferedWriter, FileWriter}

import scalatags.Text.TypedTag
import scalatags.Text.all._

import org.json4s._
import org.json4s.native.JsonMethods._

case class Attribute(
                    attribute: String,
                    required: Boolean,
                    description: String,
                    values: List[String]
                    )
case class Example(
                  title: String,
                  example: String
                  )
case class Child(
                label: String,
                description: String
                )

case class Entity(
		      name:String,
              description:String,
              label: String,
              attributes: List[Attribute],
              children: List[Child],
              notes: List[String],
              examples: List[Example],
		      tags: List[String]
              )

case class EntityTreeNode(
                         node: Entity,
                         children: List[EntityTreeNode]
                         )

object DocGen {

	lazy val Title = "title".tag[String]
	lazy val Style = "style".tag[String]
	lazy val section = "section".tag[String]
	lazy val btn = "button".tag[String]

	def usage() = {
		val message =
			"""usage: java -jar docgen.jar <input file json> <output file html>
			  |
			  |example:
			  |     java -jar docgen.jar "c:/users/me/desktop/spec.json" "c:/users/me/desktop/docs.htm"
			  |
			  |json file is an array of objects:
			  |[
			  |{
			  |      "name":"column",
			  |      "description":"Selects a column, by name, from the `src` of data and feeds it to `dst`.",
			  |      "label":"column",
			  |      "attributes":[
			  |         {
			  |            "attribute":"name",
			  |            "required":true,
			  |            "description":"The name of the column in `src` to select."
			  |         },
			  |         {
			  |            "attribute":"rename",
			  |            "required":false,
			  |            "description":"Used when using column shorthand for quick renames.  See example."
			  |         }
			  |      ],
			  |      "children":[
			  |         {
			  |            "label":"operations",
			  |            "description":"A region filled with Operations to perform on the column."
			  |         }
			  |      ],
			  |      "notes":[
			  |
			  |      ],
			  |      "examples":[
			  |         {"title": "Renaming Shorthand", "example": "<column name=\"old_name\" rename\"new_name\" />"}
			  |      ],
			  |      "tags":[
			  |         "column"
			  |      ]
			  |   }
			  |]
			""".stripMargin

		print(message)
	}

	def main(args: Array[String]): Unit=
	{
		implicit val formats = org.json4s.DefaultFormats

		if(args.length == 1) {
			System.err.println("No output file was specified")
			usage
			return
		}

		if(args.length == 0){
			System.err.println("No input files or output files specified")
			usage
			return
		}

		val infile = new File(args.head)

		val entities = parse(infile).extract[List[Entity]].sortBy(_.name)

		val tags = entities.flatMap(_.tags).toSet

		val entityDict = entities.map(entity => (entity.name, entity)).toMap

		val tree = buildEntityTree("job", entityDict, entityDict.keySet.map(key => (key, false)).toMap )
		val javascript =
			"""
			  |$(document).on('ready',function(e){
			  | console.log('ready');
			  | var $body = $('body');
			  | $(this).on('click', 'button[data-toggles]', function(e){
			  |     e.preventDefault()
			  |     $body.toggleClass( $(this).toggleClass('active').data('toggles') );
			  |
			  | });
			  |});
			""".stripMargin

		val page = html(
			head(
				link(href:="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.6/css/bootstrap.min.css", rel:="stylesheet", media:="all"),
				script(src:="https://cdn.rawgit.com/google/code-prettify/master/loader/run_prettify.js?lang=xml&skin=sunburst"),
				script(src:="https://cdnjs.cloudflare.com/ajax/libs/jquery/1.12.0/jquery.min.js"),
				script(javascript),
				Style( tags.map(tag => s".when-$tag {display:none} .slice_$tag .when-$tag {display: block}").mkString("") ),
				Title("Conduit Documentation")
			),
			body(
				cls:=tags.map(tag => s"slice_$tag").mkString(" "),
				div(
					cls:="container",
					div(
						cls:="row",
						div(cls:="col-md-12", printEntityTree(tree) )
					),
					div(
						cls:="row",
						div(cls:="col-md-12",
							div(cls:="btn-group", getTagSlicer(tags.toList))
						)
					),
					div(
						cls:="row",
						div(cls:="col-md-12", entities.map(renderEntity))
					)

				)
			)
		)

		val file = new File(args.last)
		val bw = new BufferedWriter(new FileWriter(file))
		bw.write("<!DOCTYPE html>\n"+page.toString())
		bw.close()
	}

	def getTagSlicer(tags: Seq[String]): Seq[ ConcreteHtmlTag[String] ] =
	{
		tags.sortBy(tag => tag).map(tag => button(cls:="btn btn-default btn-xs active", data.toggles:=s"slice_$tag",tag) )
	}

	def buildEntityTree(root: String, dict: Map[String, Entity], visited: Map[String, Boolean]): EntityTreeNode =
	{
		val newVisited = visited ++ Map(root->true)

		val children = if( visited(root) )
			List()
		else
			dict(root).children.map(child => buildEntityTree(child.label, dict, newVisited ) )

		EntityTreeNode( dict(root), children )
	}

	def printEntityTree(root: EntityTreeNode): ConcreteHtmlTag[String] =
	{
		ul(
			li(
				a(
					href:=s"#entity__${root.node.label}",
					if(root.children.isEmpty) s"<${root.node.name} />" else s"<${root.node.name}>"
				),
				if(root.children.isEmpty) "" else root.children.sortBy(child => child.node.name).map(child => printEntityTree(child) ),
				if(root.children.nonEmpty) a(href:=s"#entity__${root.node.label}", s"</${root.node.name}>") else ""
			)
		)
	}

	def renderEntity(entity: Entity): TypedTag[String] =
	{
		section(
			id:=s"entity__${entity.label}",
			cls:=entity.tags.map( tag => s"when-$tag" ).mkString(" "),
			hr(),
			h4(entity.name, " ", code(s"<${entity.label} />")),
			if(entity.tags.isEmpty) "" else p(
				strong("Tags: "),
				code(entity.tags.head),
				entity.tags.tail.map(tag =>  Seq(span(", "), code(tag)) )
			),
			p(entity.description),
			if(entity.notes.isEmpty) "" else ul(
				entity.notes.map(note => li(note) )
			),

			table(
				cls:="table table-bordered",
				thead(
					tr(
						th(colspan:="3", s"${entity.name} Usage")
					)
				),
				tbody(
					getAttributes(entity.attributes),
					getChildren(entity.children)
				)
			),
			if(entity.examples.isEmpty) "" else div(
				cls:="examples",
				entity.examples.map( example => Seq(
					h4(s"Example: ${example.title}"),
					pre(
						cls:="prettyprint",
						example.example
					)
				))
			)
		)
	}

	def getAttributes(attributes: Seq[Attribute]): Seq[TypedTag[String]] =
	{
		if(attributes.isEmpty)
			Seq(tr(
				th("attributes"),
				td(colspan:=2, em("none") )
			))
		else{
			val head = attributes.head
			val tail = attributes.tail
			Seq(
				tr(
					th(rowspan:=attributes.length, "attributes"),
					if(head.required) td(cls:="attr", title:="This attribute is required", strong(head.attribute) ) else td(cls:="attr", head.attribute),
					td(cls:="desc", head.description)

				)
			) ++ tail.map(attribute => tr(
				if(attribute.required) td(cls:="attr", title:="This attribute is required", strong(attribute.attribute) ) else td(cls:="attr", attribute.attribute),
				td(cls:="desc", attribute.description)
			))
		}
	}
	def getChildren(children: Seq[Child]): Seq[TypedTag[String]] =
	{
		val rs = if(children.isEmpty) 1 else children.length

		if(children.isEmpty)
			Seq(
				tr( th("children"), td(colspan:=2, em("none") ) )
			)
		else {
			val head = children.head
			val tail = children.tail
			Seq(
				tr(
					th(rowspan:=children.length, "children"),
					td( "<", a(href:=s"#entity__${head.label}", head.label), " />" ),
					td( head.description )
				)
			) ++ tail.map(child =>
				tr(
					td( "<", a(href:=s"#entity__${child.label}", child.label), " />" ),
					td( child.description )
				)
			)
		}
	}
}
