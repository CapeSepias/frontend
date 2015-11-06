package jobs

import _root_.model.Content
import com.gu.contentapi.client.model.{Content => ApiContent}
import com.gu.facia.api.models.{CollectionConfig, FaciaContent}
import common._
import conf.LiveContentApi
import implicits.Dates
import layout.{CollectionEssentials, FaciaContainer}
import org.joda.time.{DateTime, DateTimeZone}
import services.{CollectionConfigWithId, FaciaContentConvert}
import slices.{TTT, ContainerDefinition, Fixed, FixedContainers}

import scala.concurrent.Future

case class FrontPageBookSections(id: String, displayName: String)

object TodaysNewspaperJob extends ExecutionContexts with Dates with Logging {
  protected val todaysPaper = AkkaAgent[Set[FaciaContainer]](Set.empty)

  val bookSections = List(
    FrontPageBookSections("theguardian/mainsection/topstories", "Front page"),
    FrontPageBookSections("theguardian/mainsection/uknews", "UK news"),
    FrontPageBookSections("theguardian/mainsection/eyewitness", "Eyewitness"),
    FrontPageBookSections("theguardian/mainsection/international", "International"),
    FrontPageBookSections("theguardian/mainsection/financial3", "Financial") //TODO use the webTitle
  )

  def fetchTodaysPaper {
    val today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay()

    bookSections.foreach { bookSection =>

      val item = LiveContentApi.item(bookSection.id).useDate("newspaper-edition").showFields("all").showElements("all").pageSize(20).fromDate(today) //todo restrict show-fields?
      val resultsF: Future[FaciaContainer] = LiveContentApi.getResponse(item).map { resp =>
          val content = getResultsOrderByNewspaperNumber(resp.results).map(c => FaciaContentConvert.frontendContentToFaciaContent(Content(c)))
          bookSectionContainer(bookSection.id, bookSection.displayName, content, bookSections.indexOf(bookSection))
        }


      resultsF.map { results =>
        todaysPaper.alter { _ + results }
      }
    }
  }

  private def getResultsOrderByNewspaperNumber(unorderedContent: List[ApiContent]): List[ApiContent] = {
    val pageNumberToContent: List[(Int, ApiContent)] = unorderedContent.flatMap { c =>
      val pageNumberOpt = c.fields.getOrElse(Map.empty).get("newspaperPageNumber").map(_.toInt)
      pageNumberOpt.map(_ -> c)
    }
    pageNumberToContent.sortBy(_._1).map(_._2)
  }

  private def bookSectionContainer(dataId: String, displayName: String, trails: Seq[FaciaContent], index: Int): FaciaContainer = {
    val containerDefinition = trails.length match {
      case 1 => FixedContainers.fixedSmallSlowI
      case 2 => FixedContainers.fixedSmallSlowII
      case 3 => ContainerDefinition.ofSlices(TTT)
      case _ => FixedContainers.fixedMediumFastXII }

    FaciaContainer(
      index,
      Fixed(containerDefinition),
      CollectionConfigWithId(dataId, CollectionConfig.empty.copy(displayName = Some(displayName))),
      CollectionEssentials(trails, Nil, Some(displayName), Some(dataId), None, None)
    )
  }

  def getTodaysPaper = todaysPaper.get()

}
