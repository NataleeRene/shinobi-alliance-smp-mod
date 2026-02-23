package com.shinobi.shinobialliancemod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ShinobiAllianceModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register HUD renderer
        HudOverlayRenderer.register();

        // Listen for HUD update packets from server
        ClientPlayNetworking.registerGlobalReceiver(
            com.shinobi.shinobialliancemod.network.HudUpdatePayload.TYPE,
            (payload, context) -> {
                System.out.println("[Client] Received HUD data: " + payload.village() + ", " + payload.rank() + ", pts=" + payload.points() + ", claims=" + payload.claimed() + "/" + payload.limit());
                context.client().execute(() -> {
                    HudOverlayRenderer.updateData(
                        payload.village(),
                        payload.rank(),
                        payload.points(),
                        payload.claimed(),
                        payload.limit()
                    );
                    System.out.println("[Client] HUD data updated");
                });
            }
        );

        // Listen for HUD visibility packets
        ClientPlayNetworking.registerGlobalReceiver(
            com.shinobi.shinobialliancemod.network.HudVisibilityPayload.TYPE,
            (payload, context) -> {
                System.out.println("[Client] Received HUD visibility=" + payload.visible());
                context.client().execute(() -> {
                    HudOverlayRenderer.setVisible(payload.visible());
                    System.out.println("[Client] HUD visibility updated to " + payload.visible());
                });
            }
        );
    }
}
