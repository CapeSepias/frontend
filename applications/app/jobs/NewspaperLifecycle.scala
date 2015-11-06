package jobs

import common.{AkkaAsync, Jobs, Logging, ExecutionContexts}
import play.api.{Application, GlobalSettings}

trait NewspaperLifecycle extends GlobalSettings with ExecutionContexts with Logging {
  override def onStart(app: Application) {
    super.onStart(app)
    Jobs.deschedule("NewspaperRefreshJob")
    //run every minute, 5 seconds after the minute
    Jobs.schedule("NewspaperRefreshJob", "5 * * * * ?") {
      TodaysNewspaperJob.fetchTodaysPaper
    }

    AkkaAsync {
      TodaysNewspaperJob.fetchTodaysPaper
    }
  }

}
