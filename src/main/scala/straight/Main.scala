package straight

import java.awt.image.BufferedImage

import javafx.application.Application
import javafx.stage.{Stage, FileChooser}
import javafx.scene.{Scene, Group, SnapshotParameters}
import javafx.scene.control.{MenuBar, Menu, MenuItem}
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.effect.GaussianBlur
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
    gc.setEffect(null)
    gc.clearRect(0, 0, canvas.getWidth, canvas.getHeight)
    gc.setEffect(new GaussianBlur)
    gc.setLineWidth(shape.line.getStrokeWidth)
    gc.setLineCap(shape.line.getStrokeLineCap)
    val points = shape.points.sliding(2, 1).toSeq.zip(height.points)
    points.foreach {
      case ((x1, y1) +: (x2, y2) +: _, (_, z)) =>
        gc.setStroke(new Color(z, z, z, 1))
        gc.strokeLine(x1 * canvas.getWidth, y1 * canvas.getHeight, x2 * canvas.getWidth, y2 * canvas.getHeight)
      case _ =>
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
    val save = new MenuItem("Save")
    val exit = new MenuItem("Exit")
    save.setOnAction({ event =>
      val image = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY)
      image.createGraphics.drawImage(SwingFXUtils.fromFXImage(canvas.snapshot(new SnapshotParameters, null), null), 0, 0, null)
      ImageIO.write(image, "RAW", chooser.showSaveDialog(stage))
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
