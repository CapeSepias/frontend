package controllers

import common.{ExecutionContexts, Logging}
import jobs.TodaysNewspaperJob
import layout.FaciaContainer
import model.MetaData
import play.api.mvc.{Action, Controller}

object NewspaperController extends Controller with Logging with ExecutionContexts {
  //todo rename to today?
  //todo this response needs caching
  def index() = Action { implicit request =>
    val page = model.Page(request.path, "todo", "Todo", "Todo")

    val paper = TodaysNewspaperJob.getTodaysPaper
    val todaysPaper = TodayNewspaper(page, paper)
    Ok(views.html.todaysNewspaper(todaysPaper))

  }
}

//todo add metadata
case class TodayNewspaper(page: MetaData, bookSections: Set[FaciaContainer])
