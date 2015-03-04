package straight

import scala.collection.JavaConverters._

import java.awt.image.BufferedImage
import java.nio.file.Files

import javafx.application.Application
import javafx.stage.{Stage, FileChooser}
import javafx.scene.{Scene, Group, SnapshotParameters}
import javafx.scene.control.{MenuBar, Menu, MenuItem}
import javafx.scene.shape.Circle
import javafx.scene.canvas.Canvas
import javafx.scene.paint.{Color, LinearGradient, CycleMethod, Stop}
import javafx.scene.effect.GaussianBlur
import javafx.scene.input.{MouseEvent, KeyCode, KeyCombination, KeyCodeCombination}
import javafx.event.{EventHandler, ActionEvent}
import javafx.embed.swing.SwingFXUtils

import javax.imageio.ImageIO

import org.apache.commons.math3.analysis.interpolation.{SplineInterpolator, LinearInterpolator}

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

case class Properties(shape: Seq[Double], width: Double, height: Seq[Double], objects: Seq[Double])

class Main extends Application {
  val size = 513
  implicit val formats = Serialization.formats(NoTypeHints)
  val shape = new Straight("Shape")((xs, ys) => (new SplineInterpolator).interpolate(xs, ys).value)
  val height = new Straight("Height")((xs, ys) => (new LinearInterpolator).interpolate(xs, ys).value)
  val root = new Group
  val radius = 5
  def add(x: Double, y: Double) = {
    val circle = new Circle(x, y, radius, Color.YELLOW)
    circle.setOnMousePressed({ e =>
      if (e.getTarget == circle) {
        if (e.isShiftDown) {
          root.getChildren.remove(circle)
        }
      }
    }: EventHandler[MouseEvent])
    circle.setOnDragDetected({ e =>
      circle.startFullDrag
    }: EventHandler[MouseEvent])
    circle.setOnMouseDragged({ e =>
      circle.setCenterX(e.getX)
      circle.setCenterY(e.getY)
    }: EventHandler[MouseEvent])
    root.getChildren.add(circle)
  }
  val canvas = new Canvas(size, size)
  canvas.setOnMousePressed({ e =>
    if (e.getTarget == canvas) {
      add(e.getX, e.getY)
    }
  }: EventHandler[MouseEvent])
  val chooser = new FileChooser
  val gc = canvas.getGraphicsContext2D
  def draw = {
    val width = canvas.getWidth
    val height = canvas.getHeight
    val shapes = shape.points
    val heights = this.height.points
    gc.setFill(Color.BLACK)
    gc.fillRect(0, 0, width, height)
    gc.setLineWidth(shape.line.getStrokeWidth)
    gc.setLineCap(shape.line.getStrokeLineCap)
    val points = shapes.sliding(2, 1).toSeq.zip(heights)
    for ((a, b) <- List(1.0 -> 2, 0.8 -> 1)) {
      points.foreach {
        case ((x1, y1) +: (x2, y2) +: _, (_, z)) =>
          gc.setStroke(new Color((1.0 - z) * a, (1.0 - z) * a, (1.0 - z) * a, 1.0))
          gc.setLineWidth(shape.line.getStrokeWidth * b)
          gc.strokeLine(x1 * width, y1 * height, x2 * width, y2 * height)
        case _ =>
      }
    }
  }
  def show(straight: Straight, stage: Stage) = {
    straight.onChange(draw)
    straight.root.setMinWidth(size)
    straight.root.setMinHeight(size)
    straight.stage.initOwner(stage)
    straight.stage.show
  }
  def start(stage: Stage) = {
    val bar = new MenuBar
    bar.setUseSystemMenuBar(true)
    val file = new Menu("File")
    bar.getMenus.add(file)
    val open = new MenuItem("Open")
    open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN))
    open.setOnAction({ event =>
      for (file <- Option(chooser.showOpenDialog(stage))) {
        val props = read[Properties](new String(Files.readAllBytes(file.toPath)))
        shape.clear
        props.shape.sliding(2, 2).foreach {
          case x :: y :: Nil => shape.add(x * shape.root.getWidth, y * shape.root.getHeight)
          case _ =>
        }
        shape.line.setStrokeWidth(props.width)
        height.clear
        props.height.sliding(2, 2).foreach {
          case x :: y :: Nil => height.add(x * height.root.getWidth, y * height.root.getHeight)
          case _ =>
        }
        root.getChildren.removeAll(root.getChildren.asScala.collect { case circle: Circle => circle }: _*)
        props.objects.sliding(2, 2).foreach {
          case x :: y :: Nil => add(x * canvas.getWidth, y * canvas.getHeight)
          case _ =>
        }
      }
    }: EventHandler[ActionEvent])
    file.getItems.add(open)
    val save = new MenuItem("Save")
    save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN))
    save.setOnAction({ event =>
      for (file <- Option(chooser.showSaveDialog(stage))) {
        val image = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY)
        image.createGraphics.drawImage(SwingFXUtils.fromFXImage(canvas.snapshot(new SnapshotParameters, null), null), 0, 0, null)
        ImageIO.write(image, "RAW", file)
        val json = write(Properties(shape.rawPoints.flatMap { case (x, y) => Seq(x, y) }, shape.line.getStrokeWidth, height.rawPoints.flatMap { case (x, y) => Seq(x, y) }, root.getChildren.asScala.collect {
          case circle: Circle => (circle.getCenterX / canvas.getWidth, circle.getCenterY / canvas.getHeight)
        }.sorted.flatMap { case (x, y) => Seq(x, y) }))
        Files.write(file.toPath.getParent.resolve(file.getName + ".json"), json.getBytes)
      }
    }: EventHandler[ActionEvent])
    file.getItems.add(save)
    val exit = new MenuItem("Exit")
    exit.setOnAction({ event => stage.close }: EventHandler[ActionEvent])
    file.getItems.add(exit)
    root.getChildren.add(bar)
    root.getChildren.add(canvas)
    stage.setTitle("Heightmap")
    stage.setScene(new Scene(root))
    stage.show
    show(shape, stage)
    show(height, stage)
    shape.stage.setX(stage.getX + size)
    shape.stage.setY(stage.getY)
    height.stage.setX(stage.getX - size)
    height.stage.setY(stage.getY)
  }
}

object Main extends App {
  Application.launch(classOf[Main])
}
