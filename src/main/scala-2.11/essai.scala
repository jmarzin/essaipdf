/**
  * Created by jacquesmarzin on 28/02/2017.
  */
import java.io.File
import java.awt.geom.Point2D
import javax.swing.text.Document

import org.pdfclown.documents.Page
import org.pdfclown.documents.contents.composition.PrimitiveComposer
import org.pdfclown.documents.contents.fonts.{Font, StandardType1Font}
import org.pdfclown.documents.contents.{ContentScanner, Contents}
import org.pdfclown.documents.contents.objects._
import org.pdfclown.files._
import org.pdfclown.objects.PdfName
import org.pdfclown.tools.TextExtractor

import scala.collection.mutable
import scala.swing.{Dialog, FileChooser}

object essai extends App{
  case class Ligne(var y: Double, var texte: String) {
  }

  var adresseAtd = List[Ligne]()

  def scan(level: ContentScanner, page: Page, capture: Boolean, ligne: Ligne) : Ligne = {
    var captureC = capture
    var ligneC = ligne
    if(level == null) return ligneC
    var supprime = false
    while(supprime || level.moveNext()) {
      val objet = level.getCurrent
      supprime = false
      if(captureC){
        if(objet.isInstanceOf[BeginSubpath]) {
          ligneC.y = objet.asInstanceOf[BeginSubpath].getPoint.getY
        } else if (objet.isInstanceOf[Text]) {
          val fonte = objet.asInstanceOf[Text].getObjects.get(0).asInstanceOf[SetFont].getFont(level.getContentContext)
          val texte = objet.asInstanceOf[Text].getObjects.get(2)
          ligneC.texte = if (texte.isInstanceOf[ShowSimpleText])
            fonte.decode(texte.asInstanceOf[ShowSimpleText].getText)
          else
            fonte.decode(texte.asInstanceOf[ShowAdjustedText].getText)
        }
      } else if(objet.isInstanceOf[LocalGraphicsState]) {
        val objet1 = objet.asInstanceOf[LocalGraphicsState].getObjects.get(0)
        if (objet1.isInstanceOf[Path]) {
          val objet2 = objet1.asInstanceOf[Path].getObjects.get(0)
          if (objet2.isInstanceOf[BeginSubpath] && objet2.asInstanceOf[BeginSubpath].getPoint.getX == 2374d){
            captureC = true
            ligneC = new Ligne(0, "")
            ligneC = scan(level.getChildLevel(), page, captureC, ligneC)
            adresseAtd = adresseAtd :+ ligneC
            captureC = false
            level.remove()
            level.getCurrent
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.remove(3)
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.remove(2)
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.remove(1)
            //objet.asInstanceOf[LocalGraphicsState].getObjects.get(0).asInstanceOf[Path].getObjects.set(0, new BeginSubpath(2374d,4374d))
            //level.setCurrent(objet)
            supprime = true
          }
        }
      }
      if(!supprime && level.getChildLevel != null) {
        scan(level.getChildLevel(), page, captureC, ligneC)
      }
    }
    ligneC
  }
  val fichier = new org.pdfclown.files.File("ATD.pdf")
  val document = fichier.getDocument
  val fonteOCR = Font.get(document, "ocr.ttf")

  val pages = document.getPages
  val page1 = pages.get(0)
  var contenus = page1.getContents
  var extrait = new TextExtractor(false,true).extract(page1.getContents).get(null)
  var textesExtraits = (for(ip <- 0 until extrait.size()) yield extrait.get(ip).getText).toArray
  var aller = false
  for (i <- textesExtraits.indices) {
    if(!aller) {
      if (textesExtraits(i).startsWith("DIRECTION")) aller = true
      textesExtraits(i) = ""
    } else if(textesExtraits(i).startsWith("Tél.")) {
      aller = false
      textesExtraits(i) = ""
    } else if(textesExtraits(i).startsWith("CS ")) textesExtraits(i) = ""
  }
  val adresseSie = textesExtraits.filter(_.nonEmpty)
  var scanner = new ContentScanner(contenus)
  scan( scanner, // Wraps the page contents into the scanner.
    page1, capture = false, null)
  var composer = new PrimitiveComposer(scanner)
  composer.setFont( new StandardType1Font( document, StandardType1Font.FamilyEnum.Courier, true, false), 10 )
  composer.showText("###", new Point2D.Double(10d,825d))
  composer.setFont(fonteOCR,10)
  var y = 190d
  adresseAtd.foreach(ligne  => {
    composer.showText(ligne.texte, new Point2D.Double(295d,y))
    y += 10
  })
  composer.setFont( new StandardType1Font( document, StandardType1Font.FamilyEnum.Courier, true, false), 8 )
  y = 110d
  adresseSie.foreach(ligne => {
    composer.showText(ligne, new Point2D.Double(295d,y))
    y += 8
  })
  contenus.flush()
  fichier.save("ATDC.pdf",SerializationModeEnum.Standard)
  fichier.close()
}
