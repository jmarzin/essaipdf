/**
  * Created by jacquesmarzin on 28/02/2017.
  */
import java.io.File

import java.awt.geom.Point2D
import org.pdfclown.documents.Page
import org.pdfclown.documents.contents.composition.PrimitiveComposer
import org.pdfclown.documents.contents.fonts.StandardType1Font
import org.pdfclown.documents.contents.{ContentScanner, Contents}
import org.pdfclown.documents.contents.objects._
import org.pdfclown.files._
import org.pdfclown.tools.TextExtractor

import scala.collection.mutable
import scala.swing.{Dialog, FileChooser}

object essai extends App{

  def scan(level: ContentScanner, page: Page, capture: Boolean) : Unit = {
    var captureC = capture
    if(level == null) return
    var supprime = false
    while(supprime || level.moveNext()) {
      val objet = level.getCurrent
      supprime = false
      if(objet.isInstanceOf[LocalGraphicsState]) {
        val objet1 = objet.asInstanceOf[LocalGraphicsState].getObjects.get(0)
        if (objet1.isInstanceOf[Path]) {
          val objet2 = objet1.asInstanceOf[Path].getObjects.get(0)
          if (objet2.isInstanceOf[BeginSubpath] && objet2.asInstanceOf[BeginSubpath].getPoint.getX == 2374d){
            captureC = true
            scan(level, page, captureC)
            captureC = false
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.remove(3)
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.remove(2)
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.remove(1)
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.set(0, new BeginSubpath(2374d,4374d))
            //level.setCurrent(objet)
            supprime = false
            println("modifié")
          }
        }
      }
      if(objet.isInstanceOf[ContainerObject]) {
        scan(level.getChildLevel(), page, captureC)
      }
    }
  }

  val fichier = new org.pdfclown.files.File("ATD.pdf")
  val document = fichier.getDocument
  val pages = document.getPages
  val page1 = pages.get(0)
  var contenus = page1.getContents
  var scanner = new ContentScanner(contenus)
  scan( scanner, // Wraps the page contents into the scanner.
    page1, capture = false)
  var composer = new PrimitiveComposer(scanner)
  composer.setFont(
    new StandardType1Font(
      document,
      StandardType1Font.FamilyEnum.Courier,
      true,
      false
    ),
    8
  )
  composer.showText("###", new Point2D.Double(10d,820d))
  contenus.flush()
  fichier.save("ATDC.pdf",SerializationModeEnum.Standard)
  fichier.close()
}
