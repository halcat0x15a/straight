package straight

import scala.collection.JavaConverters._

import javafx.stage.Stage
import javafx.scene.{Node, Scene, Group}
import javafx.scene.shape._
import javafx.scene.layout.{AnchorPane, VBox}
import javafx.scene.input.MouseEvent
import javafx.event.EventHandler
import javafx.collections.ListChangeListener
import javafx.beans.value.ChangeListener

class Straight(title: String)(interpolator: (Array[Double], Array[Double]) => Double => Double) {
  type Point = (Double, Double)
  val minWidth = 5
  val extraRadius = 2
  val root = new AnchorPane
  def reset(node: Node): Unit = root.getChildren.set(root.getChildren.indexOf(node), node)
  def onChange[A](f: => A): Unit = root.getChildren.addListener({ change => f }: ListChangeListener[Node])
  def clear = root.getChildren.removeAll(root.getChildren.asScala.collect { case circle: Circle => circle }: _*)
  def add(x: Double, y: Double) = {
    val circle = new Circle(x, y, minWidth + extraRadius)
    circle.radiusProperty.bind(line.strokeWidthProperty.add(extraRadius))
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
      reset(circle)
    }: EventHandler[MouseEvent])
    root.getChildren.add(circle)
  }
  def rawPoints: Seq[Point] =
    root.getChildren.asScala.collect {
      case circle: Circle => circle.getCenterX / root.getWidth -> circle.getCenterY / root.getHeight
    }.sorted
  def points: Seq[Point] = {
    val points = rawPoints
    if (points.size <= 2) {
      Nil
    } else {
      val xs = points.map(_._1)
      val ys = points.map(_._2)
      val f = interpolator(xs.toArray, ys.toArray)
      for (x <- xs.min until xs.max by 0.001) yield x -> f(x)
    }
  }
  val line = new Polyline
  onChange {
    line.getPoints.clear
    points foreach {
      case (x, y) => line.getPoints.addAll(x * root.getWidth, y * root.getHeight)
    }
  }
  line.setStrokeLineCap(StrokeLineCap.ROUND)
  line.setStrokeLineJoin(StrokeLineJoin.ROUND)
  line.setStrokeWidth(minWidth)
  line.setOnMousePressed({ e =>
    if (e.getTarget == line) {
      if (e.isShiftDown) {
        if (line.getStrokeWidth > minWidth)
          line.setStrokeWidth(line.getStrokeWidth - 1)
      } else {
        line.setStrokeWidth(line.getStrokeWidth + 1)
      }
      reset(line)
    }
  }: EventHandler[MouseEvent])
  root.setOnMousePressed({ e =>
    if (e.getTarget == root) {
      add(e.getX, e.getY)
    }
  }: EventHandler[MouseEvent])
  root.getChildren.add(line)
  val stage = new Stage
  stage.setTitle(title)
  stage.setResizable(true)
  root.minHeightProperty.bind(stage.widthProperty())
  root.minWidthProperty.bind(stage.heightProperty())
  stage.setScene(new Scene(root))
}

