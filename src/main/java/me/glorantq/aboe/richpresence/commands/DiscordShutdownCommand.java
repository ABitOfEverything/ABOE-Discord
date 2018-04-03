package me.glorantq.aboe.richpresence.commands;

import me.glorantq.aboe.richpresence.ModMain;
import net.minecraft.command.ICommandSender;

public class DiscordShutdownCommand extends ClientOnlyDeveloperCommand {
    private ModMain mod = ModMain.getInstance();

    @Override
    protected String getCommandBase() {
        return "discord_rpc_shutdown";
    }

    @Override
    public void processCommand(ICommandSender p_71515_1_, String[] p_71515_2_) {
        mod.getDiscordRPC().Discord_ClearPresence();
        mod.getDiscordRPC().Discord_Shutdown();

        sendChatMessage(p_71515_1_, "Shut down Discord RPC!");
    }
}
