package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{api, Settings}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import scala.collection.convert.WrapAsScala._
import net.minecraftforge.common.util.Constants.NBT

class WirelessRouter extends Router with WirelessEndpoint {
  var strength = Settings.get.maxWirelessRange

  val componentNodes = Array.fill(6)(api.Network.newNode(this, Visibility.Network).withComponent("access_point").create())

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when relaying messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = synchronized(result(strength))

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when relaying messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = synchronized {
    strength = math.max(args.checkDouble(0), math.min(0, Settings.get.maxWirelessRange))
    result(strength)
  }

  // ----------------------------------------------------------------------- //

  override def receivePacket(packet: Packet, distance: Double) {
    if (queue.size < 20) {
      queue += ForgeDirection.UNKNOWN -> packet.hop()
    }
    packet.data.headOption match {
      case Some(answerPort: java.lang.Double) => queueMessage(packet.source, packet.destination, packet.port, answerPort.toInt, packet.data.drop(1))
      case _ => queueMessage(packet.source, packet.destination, packet.port, -1, packet.data)
    }
  }

  override protected def relayPacket(sourceSide: ForgeDirection, packet: Packet) {
    super.relayPacket(sourceSide, packet)
    if (sourceSide != ForgeDirection.UNKNOWN && strength > 0) {
      if (sourceSide == null || {
        val cost = Settings.get.wirelessCostPerRange
        val connector = plugs(sourceSide.ordinal).node.asInstanceOf[Connector]
        connector.tryChangeBuffer(-strength * cost)
      }) api.Network.sendWirelessPacket(this, strength, packet)
    }
  }

  override protected def createNode(plug: Plug) = api.Network.newNode(plug, Visibility.Network).withConnector().create()

  // ----------------------------------------------------------------------- //

  override protected def onPlugConnect(plug: Plug, node: Node) {
    super.onPlugConnect(plug, node)
    if (node == plug.node) {
      api.Network.joinWirelessNetwork(this)
    }
    if (!node.network.nodes.exists(componentNodes.contains)) {
      node.connect(componentNodes(plug.side.ordinal))
    }
  }

  override protected def onPlugDisconnect(plug: Plug, node: Node) {
    super.onPlugDisconnect(plug, node)
    if (node == plug.node) {
      api.Network.leaveWirelessNetwork(this)
      componentNodes(plug.side.ordinal).remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    if (nbt.hasKey(Settings.namespace + "strength")) {
      strength = nbt.getDouble(Settings.namespace + "strength") max 0 min Settings.get.maxWirelessRange
    }
    nbt.getTagList(Settings.namespace + "componentNodes", NBT.TAG_COMPOUND).foreach {
      case (list, index) => componentNodes(index).load(list.getCompoundTagAt(index))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    nbt.setDouble(Settings.namespace + "strength", strength)
    nbt.setNewTagList(Settings.namespace + "componentNodes", componentNodes.map(node => {
      val tag = new NBTTagCompound()
      node.save(tag)
      tag
    }))
  }
}
