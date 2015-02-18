package straight

import scala.collection.JavaConverters._

import java.awt.image.BufferedImage

import javafx.application.Application
import javafx.stage.{Stage, FileChooser}
import javafx.scene.{Scene, Group, SnapshotParameters}
import javafx.scene.control.{MenuBar, Menu, MenuItem}
import javafx.scene.canvas.Canvas
import javafx.scene.paint.{Color, LinearGradient, CycleMethod, Stop}
import javafx.scene.effect.GaussianBlur
import javafx.scene.input.{KeyCode, KeyCombination, KeyCodeCombination}
import javafx.event.{EventHandler, ActionEvent}
import javafx.embed.swing.SwingFXUtils

import javax.imageio.ImageIO

class Main extends Application {
  val size = 513
  val shape = new Straight("Shape")
  val height = new Straight("Height")
  val root = new Group
  val canvas = new Canvas(size, size)
  val chooser = new FileChooser
  val gc = canvas.getGraphicsContext2D
  def draw = {
    val width = canvas.getWidth
    val height = canvas.getHeight
    val shapes = shape.points.toArray
    val heights = this.height.points
    gc.setFill(Color.BLACK)
    gc.fillRect(0, 0, width, height)
    gc.setLineCap(shape.line.getStrokeLineCap)
    def stops(base: Double) = 
      heights.map {
        case (x, y) =>
          val v = if (base - y < 0) 0 else base - y
          new Stop(x, new Color(v, v, v, 1.0))
      }
    val xs = shapes.map(_._1 * width)
    val ys = shapes.map(_._2 * height)
    gc.setLineWidth(shape.line.getStrokeWidth * 2)
    gc.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops(1): _*))
    gc.strokePolyline(xs, ys, shapes.size)
    gc.setLineWidth(shape.line.getStrokeWidth)
    gc.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops(0.9): _*))
    gc.strokePolyline(xs, ys, shapes.size)
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
    val save = new MenuItem("Save")
    val exit = new MenuItem("Exit")
    save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN))
    save.setOnAction({ event =>
      for (file <- Option(chooser.showSaveDialog(stage))) {
        val image = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY)
        image.createGraphics.drawImage(SwingFXUtils.fromFXImage(canvas.snapshot(new SnapshotParameters, null), null), 0, 0, null)
        ImageIO.write(image, "RAW", file)
      }
    }: EventHandler[ActionEvent])
    file.getItems.add(save)
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
