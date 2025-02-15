package yesman.epicfight.network.client;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class CPExecuteSkill {
	private int skillSlot;
	private boolean active;
	private FriendlyByteBuf buffer;

	public CPExecuteSkill() {
		this(0);
	}

	public CPExecuteSkill(int slotIndex) {
		this(slotIndex, true);
	}
	
	public CPExecuteSkill(int slotIndex, boolean active) {
		this.skillSlot = slotIndex;
		this.active = active;
		this.buffer = new FriendlyByteBuf(Unpooled.buffer());
	}
	
	public CPExecuteSkill(int slotIndex, boolean active, FriendlyByteBuf pb) {
		this.skillSlot = slotIndex;
		this.active = active;
		this.buffer = new FriendlyByteBuf(Unpooled.buffer());
		if(pb != null) {
			this.buffer.writeBytes(pb);
		}
	}

	public FriendlyByteBuf getBuffer() {
		return buffer;
	}

	public static CPExecuteSkill fromBytes(FriendlyByteBuf buf) {
		CPExecuteSkill msg = new CPExecuteSkill(buf.readInt(), buf.readBoolean());

		while (buf.isReadable()) {
			msg.buffer.writeByte(buf.readByte());
		}
		
		return msg;
	}

	public static void toBytes(CPExecuteSkill msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.skillSlot);
		buf.writeBoolean(msg.active);
		
		while (msg.buffer.isReadable()) {
			buf.writeByte(msg.buffer.readByte());
		}
	}
	
	public static void handle(CPExecuteSkill msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer serverPlayer = ctx.get().getSender();
			ServerPlayerPatch playerpatch = (ServerPlayerPatch) serverPlayer.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY, null).orElse(null);

			if (msg.active) {
				playerpatch.getSkill(msg.skillSlot).requestExecute(playerpatch, msg.getBuffer());
			} else {
				Skill contain = playerpatch.getSkill(msg.skillSlot).getSkill();
				if (contain != null) {
					contain.cancelOnServer(playerpatch, msg.getBuffer());
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}