package service

import scala.collection.mutable

class EventService(socketService: SocketService) {

  val messagesBySeqNo = new mutable.HashMap[Long, List[String]] // get rid of mutable?
  val followRegistry = new mutable.HashMap[Long, Set[Long]] // get rid of mutable?



}
