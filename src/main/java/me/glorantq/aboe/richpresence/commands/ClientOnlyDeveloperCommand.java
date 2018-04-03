package me.glorantq.aboe.richpresence.commands;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

public abstract class ClientOnlyDeveloperCommand implements ICommand {
    @Override
    public String getCommandName() {
        return "devonly!aboe_discord_" + getCommandBase();
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "";
    }

    @Override
    public List getCommandAliases() {
        return new ArrayList();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender p_71519_1_) {
        return true;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender p_71516_1_, String[] p_71516_2_) {
        return new ArrayList();
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    protected void sendChatMessage(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText("§6[§eABOE-Discord§6]§e " + message));
    }

    protected abstract String getCommandBase();
}
