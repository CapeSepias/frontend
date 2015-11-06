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
import slices.{Fixed, FixedContainers}

import scala.concurrent.Future


object TodaysNewspaperJob extends ExecutionContexts with Dates with Logging {
  protected val todaysPaper = AkkaAgent[Set[FaciaContainer]](Set.empty)

  def fetchTodaysPaper {
    val today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay()

    val mainSection = "theguardian/mainsection/topstories" //todo add other main sections
    val mainSectionName = "Front page"


    val item = LiveContentApi.item(mainSection).useDate("newspaper-edition").showFields("all").showElements("all").pageSize(20).fromDate(today) //todo restrict show-fields?
    val resultsF: Future[FaciaContainer] = LiveContentApi.getResponse(item).map { resp =>
      val content = getResultsOrderByNewspaperNumber(resp.results).map( c => FaciaContentConvert.frontendContentToFaciaContent(Content(c)))
      bookSectionContainer(mainSection, mainSectionName, content)
    }

    resultsF.map { results =>
      todaysPaper.alter { _ + results }
    }
  }

  private def getResultsOrderByNewspaperNumber(unorderedContent: List[ApiContent]): List[ApiContent] = {
    val pageNumberToContent: List[(Int, ApiContent)] = unorderedContent.flatMap { c =>
      val pageNumberOpt = c.fields.getOrElse(Map.empty).get("newspaperPageNumber").map(_.toInt)
      pageNumberOpt.map(_ -> c)
    }
    pageNumberToContent.sortBy(_._1).map(_._2)
  }

  private def bookSectionContainer(dataId: String, displayName: String, trails: Seq[FaciaContent]): FaciaContainer = {

    //todo define with K & M
    val containerDefinition = FixedContainers.fixedMediumFastXII

    FaciaContainer(
      0,
      Fixed(containerDefinition),
      CollectionConfigWithId(dataId, CollectionConfig.empty.copy(displayName = Some(displayName))),
      CollectionEssentials(trails, Nil, Some(displayName), Some(dataId), None, None)
    )
  }

  def getTodaysPaper = todaysPaper.get()

}
