package eventus.service

import eventus.common.AppError
import eventus.common.types.{CommunityId, EventId}
import eventus.dto.EventCreateDTO
import eventus.model.Event
import eventus.repository.EventRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{IO, URLayer, ZIO, ZLayer}

case class EventServiceImpl(repo: EventRepository) extends EventService {
  override def getAllOrByCommunityId(
      communityIdOpt: Option[CommunityId]
  ): IO[AppError, List[Event]] = {
    repo.getAllOrFilterByCommunityId(communityIdOpt)
  }

  override def getById(id: EventId): IO[AppError, Option[Event]] = {
    repo.filterById(id)
  }

  override def create(
      communityId: CommunityId,
      eventCreateDTO: EventCreateDTO
  ): ZIO[MemberService with NotificationService, AppError, EventId] =
    for {
      id <- zio.Random.nextUUID
      event = eventCreateDTO
        .into[Event]
        .withFieldConst(_.id, EventId(id))
        .withFieldConst(_.communityId, communityId)
        .transform
      _ <- repo.insert(event)
      _ <- NotificationService(_.notifyAboutEvent(event))
    } yield event.id

  override def update(event: Event): IO[AppError, Unit] = {
    repo.update(event)
  }
}

object EventServiceImpl {
  val live: URLayer[EventRepository, EventService] =
    ZLayer.fromFunction(EventServiceImpl(_))
}
