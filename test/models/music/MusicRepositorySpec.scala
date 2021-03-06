package models.music

import models.{WithTestUser, ModelSpecBase}
import models.util.RequireTextValidator
import models.chat.ChatRoom
import models.CoreSchema._
import java.io.File
import org.squeryl.PrimitiveTypeMode._

/**
 * Created with IntelliJ IDEA.
 * User: natsuki
 * Date: 2013/03/23
 * Time: 0:49
 * To change this template use File | Settings | File Templates.
 */
class MusicRepositorySpec extends ModelSpecBase {
  val testdata = "test/data/test-data.mp3"
  lazy val validMusic = Music(new File(testdata))
  lazy val invalidMusic = new Music("", Array.emptyByteArray)

  "MusicRepository" should {
    ".insert" >> {
      "追加可能な場合はRightオブジェクトが取得できる" >> new WithData {
        MusicRepository.insert(chatRoom, validMusic) must beRight(validMusic)
      }
      "追加不可能な場合はLeftオブジェクトが取得できる" >> new WithData {
        MusicRepository.insert(chatRoom, invalidMusic) must beLeft(Map('name -> List(RequireTextValidator.KEY), 'rawData -> List('empty)))
      }
    }
    ".find" >> {
      "指定したIDの音楽が存在する場合は、取得できる" >> new WithData {
        MusicRepository.find(music.id) must beSome(music)
      }
    }
    ".delete" >> {
      "指定したIDの音楽が存在する場合は、削除できる" >> new WithData {
        MusicRepository.delete(music.id) must beTrue
        musics.lookup(music.id) must beNone
      }
      "指定したIDの音楽が存在しない場合は、削除できない" >> new WithData {
        MusicRepository.delete(-1) must beFalse
        musics.lookup(music.id) must beSome(music)
      }
    }
  }

  trait WithData extends WithTransaction with WithTestUser {
    lazy val chatRoom = ChatRoom("aaa", user)
    var music = Music(new File(testdata))

    override def before = {
      saveUser
      chatRoom.save
      chatRoom.musics.associate(music)
    }
    override def after = {
      musics.deleteWhere(m => m.id <> 0)
      chatRooms.deleteWhere(c => c.id === chatRoom.id)
      cleanUser
    }
  }
}
