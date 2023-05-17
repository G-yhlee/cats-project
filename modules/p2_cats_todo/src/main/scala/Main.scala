import cats.effect.{IO, IOApp}
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.effect.kernel.Ref
import cats.syntax.functor.toFunctorOps
import cats.FlatMap.nonInheritedOps.toFlatMapOps
import scala.io.StdIn
import java.util.UUID
import cats.Monad

case class TodoItem(id: Int, description: String, completed: Boolean)
trait TodoRepository[F[_]] {
  def create(description: String, completed: Boolean): F[TodoItem]
  def get(id: Int): F[Option[TodoItem]]
  def update(item: TodoItem): F[Unit]
  def delete(id: Int): F[Unit]
}

class InMemoryTodoRepository[F[_]: Sync] extends TodoRepository[F] {
  private val items: Ref[F, Map[Int, TodoItem]] = Ref.unsafe(Map.empty)

  def create(description: String, completed: Boolean): F[TodoItem] =
    for {
      id <- Sync[F].delay(UUID.randomUUID().hashCode())
      item = TodoItem(id, description, completed)
      _ <- items.update(_ + (id -> item))
    } yield item

  def get(id: Int): F[Option[TodoItem]] =
    items.get.map(_.get(id))

  def update(id: Int, description: String, completed: Boolean): F[Unit] =
    for {
      maybeItem <- get(id)
      _ <- maybeItem match {
        case Some(item) =>
          val updatedItem = item.copy(description = description, completed = completed)
          items.update(_ + (id -> updatedItem)).void
        case None =>
          Sync[F].raiseError(new Exception(s"Item $id not found"))
      }
    } yield ()

  def delete(id: Int): F[Unit] =
    items.update(_ - id).void

  // def list(): F[List[TodoItem]] =
  //   items.get.map(_.values.toList)
}



trait TodoService[F[_]] {
    def create(description: String): F[TodoItem]
    def get(id: Int): F[Option[TodoItem]]
    def update(id: Int, description: String, completed: Boolean): F[TodoItem]
    def delete(id: Int): F[TodoItem]
  }

class TodoServiceImpl[F[_]: Sync](repository: TodoRepository[F])
    extends TodoService[F] {
  def create(description: String): F[TodoItem] =
    repository.create( description,  false)

  def get(id: Int): F[Option[TodoItem]] =
    repository.get(id)

  def update(id: Int, description: String, completed: Boolean): F[Unit] =
    repository.get(id).flatMap {
      case Some(item) =>
        val updatedItem =
          item.copy(description = description, completed = completed)
        repository.update(updatedItem)
      case None =>
        Sync[F].unit
    }

  def delete(id: Int): F[Unit] =
    repository.delete(id)
}


 trait TodoCli[F[_]] {
    def run(): F[Unit]
  }

class TodoCliImpl[F[_]: Sync: Monad](service: TodoService[F]) extends TodoCli[F] {
  def run(): F[Unit] =
    for {
      _ <- Sync[F].delay(println("Welcome to the Todo List App!"))
      _ <- Sync[F].delay(println("Type 'create <description>' to create a new todo item"))
      _ <- Sync[F].delay(println("Type 'get <id>' to get a todo item"))
      _ <- Sync[F].delay(println("Type 'update <id> <description> <completed>' to update a todo item"))
      _ <- Sync[F].delay(println("Type 'delete <id>' to delete a todo item"))
      _ <- Sync[F].delay(println("Type 'quit' to exit the program"))
      _ <- readCommand()
    } yield ()

  def readCommand(): F[Unit] =
    for {
      input <- Sync[F].delay(StdIn.readLine())
      _ <- input.split(" ").toList match {
        case "create" :: description :: Nil =>
          service.create(description)
        case "get" :: id :: Nil =>
          service.get(id.toInt).flatMap {
            case Some(item) =>
              Sync[F].delay(println(s"${item.id}: ${item.description}, completed=${item.completed}"))
            case None =>
              Sync[F].delay(println(s"Item $id not found"))
          }
        case "update" :: id :: description :: completed :: Nil =>
          service.update(id.toInt, description, completed.toBoolean)
        case "delete" :: id :: Nil =>
          service.delete(id.toInt)
        case "quit" :: Nil =>
          Sync[F].unit
        case _ =>
          Sync[F].delay(println("Invalid command"))
      }
      _ <- if (input == "quit") {
        Sync[F].unit
      } else {
        readCommand()
      }
    } yield ()
}



object Main extends IOApp :
  def run(args: List[String]): IO[ExitCode] = 
    for {
      repository <- IO(new InMemoryTodoRepository[IO])
      service = new TodoServiceImpl(repository)
      cli = new TodoCliImpl(service)
      _ <- cli.run()
    } yield ExitCode.Success
