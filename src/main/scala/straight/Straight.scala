package straight

import scala.collection.JavaConverters._

import javafx.stage.Stage
import javafx.scene.{Node, Scene, Group}
import javafx.scene.shape._
import javafx.scene.layout.{AnchorPane, VBox}
import javafx.scene.input.{MouseEvent, MouseDragEvent}
import javafx.event.EventHandler
import javafx.collections.ListChangeListener

class Straight(title: String)(interpolator: (Array[Double], Array[Double]) => Double => Double) {
  type Point = (Double, Double)
  val minWidth = 5
  val root = new AnchorPane
  val line = new Polyline
  val stage = new Stage
  def reset(node: Node): Unit = root.getChildren.set(root.getChildren.indexOf(node), node)
  def onChange[A](f: => A): Unit = root.getChildren.addListener({ change => f }: ListChangeListener[Node])
  def points: Seq[Point] = {
    val points = root.getChildren.asScala.collect {
      case circle: Circle => circle.getCenterX / root.getWidth -> circle.getCenterY / root.getHeight
    }.sorted
    if (points.size <= 2) {
      points
    } else {
      val xs = points.map(_._1)
      val ys = points.map(_._2)
      val f = interpolator(xs.toArray, ys.toArray)
      for (x <- xs.min until xs.max by 0.001) yield x -> f(x)
    }
  }
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
      val circle = new Circle(e.getX, e.getY, minWidth * 1.5)
      circle.radiusProperty.bind(line.strokeWidthProperty.multiply(1.5))
      circle.setOnMousePressed({ e =>
        if (e.getTarget == circle) {
          if (e.isAltDown) {
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
  }: EventHandler[MouseEvent])
  root.getChildren.add(line)
  stage.setTitle(title)
  stage.setScene(new Scene(root))
}

