package eventus.service

import eventus.common.types.CommunityId
import eventus.dto.CommunityCreateDTO
import eventus.error.{AppError, ServiceError}
import eventus.model.Community
import eventus.repository.CommunityRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{IO, URLayer, ZLayer}

import java.net.URLDecoder
import java.util.UUID

case class CommunityServiceImpl(repo: CommunityRepository)
    extends CommunityService {
  override def getAll: IO[AppError, List[Community]] = {
    repo.queryAll
  }

  override def getById(id: CommunityId): IO[AppError, Option[Community]] = {
    repo.filterById(id)
  }

  override def create(
      communityCreateDTO: CommunityCreateDTO
  ): IO[AppError, CommunityId] = for {
    id <- zio.Random.nextUUID
    community = communityCreateDTO
      .into[Community]
      .withFieldConst(_.id, CommunityId(id))
      .transform
    _ <- repo.insert(community)
  } yield community.id

  override def update(community: Community): IO[AppError, Unit] = {
    repo.update(community)
  }

  // TODO - временно так себе реализация поиска, заменить на эффективный сервис поиска
  override def search(string: String): IO[AppError, List[Community]] = {
    for {
      words <- IO
        .attempt {
          val words = URLDecoder
            .decode(string, "UTF-8")
            .split("\\s")
            .toList
            .filter(_.length > 1)
            .map(w => s"%${w.toLowerCase}%")
          if (words.isEmpty)
            throw new RuntimeException("Search string is too short")
          words
        }
        .mapError(ServiceError)
      result <- repo.likeByWordsArray(words)
    } yield result
  }
}

object CommunityServiceImpl {
  val live: URLayer[CommunityRepository, CommunityService] =
    ZLayer.fromFunction(CommunityServiceImpl(_))
}
