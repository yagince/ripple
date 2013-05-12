package models.chat

import models.{WithTestUser, ModelSpecBase}
import org.squeryl.PrimitiveTypeMode._
import models.CoreSchema._
import models.chat.action.Talk
import util.redis.RedisClient
import org.json4s.jackson.JsonMethods._

class ChatRoomRepositorySpec extends ModelSpecBase {
  "ChatRoomRepository" should {
    "#find" >> {
      "指定された名前の部屋が存在する場合はCharRoomオブジェクトを返す" >> new WithTestData {
        ChatRoomRepository.find(chatRoom.name) must beSome(chatRoom)
      }
      "指定された名前の部屋が存在しない場合は何も返さない" >> new WithTestData {
        ChatRoomRepository.find("hogefoobar") must beNone
      }
    }
    "#find(id)" >> {
      "指定したIDの部屋が存在する場合はSome(ChatRoom)が取得できる" >> new WithTestData {
        ChatRoomRepository.find(chatRoom.id) must beSome(chatRoom)
      }
      "指定したIDの部屋が存在しない場合はNoneが返る" >> new WithTestData {
        ChatRoomRepository.find(-1) must beNone
      }
    }

    "#create" >> {
      "指定されたChatRoomオブジェクトが追加可能な場合はRightオブジェクトが取得できる" >> new WithTestData {
        // TODO: 本当はstub化したいが、mixinしたtraitのメソッドをstub化しようとすると、エラーになる。UninitializedFieldError: Uninitialized field: Schema.scala: 11 (Validations.scala:23)
//        val mockChatRoom = mock[ChatRoom]
//        mockChatRoom.validate returns true
        val mockChatRoom = ChatRoom("hoge-chat", user)
        ChatRoomRepository.insert(mockChatRoom) must beRight(mockChatRoom)
        ChatRoomRepository.find(mockChatRoom.name) must beSome
      }
      "指定されたChatRoomオブジェクトが追加不可能な場合はLeftオブジェクトが取得できる" >> new WithTestData {
        // TODO: stub化
        val mockChatRoom = ChatRoom(chatRoom.name, user)
        ChatRoomRepository.insert(mockChatRoom) must beLeft
        ChatRoomRepository.find(mockChatRoom.name) must beSome(chatRoom)
      }
    }

    "#all" >> {
      "全ての部屋が取得できる" >> new WithTestData {
        ChatRoomRepository.all.size must equalTo(2)
      }
    }

    "#talk" >> {
      "Redisにstoreされる" >> new WithTestData {
        import org.sedis.Dress._

        val talk = Talk(user, "hoge")
        ChatRoomRepository.insert(chatRoom, talk).toOption must beSome(talk)
        RedisClient.withClient{ client =>
          val talks = client.lrange(talk.key(chatRoom), 0, -1)
          talks.size must_== 1
          talks.foreach{(value) =>
            value must_== compact(render(talk.toJson))
          }
        }

        override def after {
          super.after
          RedisClient.withClient{ client=>
            client.del(talk.key(chatRoom))
          }
        }
      }
    }

  }

  trait WithTestData extends WithTransaction with WithTestUser {
    lazy val chatRoom = ChatRoom("test_room", user)
    lazy val chatRoom2 = ChatRoom("test_room2", user)
    override def before = {
      saveUser
      chatRoom.save
      chatRoom2.save
    }

    override def after = {
      chatRooms.deleteWhere(c => c.id gt 0)
      cleanUser
    }
  }
}
