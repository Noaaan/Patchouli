package vazkii.patchouli.common.handler;

import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.server.MinecraftServer;

import net.minecraft.server.integrated.IntegratedServer;
import vazkii.patchouli.common.mixin.MixinMinecraftServer;
import vazkii.patchouli.common.network.message.MessageReloadBookContents;

public class ReloadContentsHandler {
	public static void init() {
		ServerStartCallback.EVENT.register(ReloadContentsHandler::serverStart);
	}

	private static void serverStart(MinecraftServer server) {
		// Also reload contents when someone types /reload
		SynchronousResourceReloadListener listener = m -> MessageReloadBookContents.sendToAll(server);
		((ReloadableResourceManager) ((MixinMinecraftServer) server).getServerResourceManager().getResourceManager())
						.registerListener(listener);
	}
}
