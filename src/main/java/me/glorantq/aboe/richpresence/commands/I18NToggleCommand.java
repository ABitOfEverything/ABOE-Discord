package me.glorantq.aboe.richpresence.commands;

import me.glorantq.aboe.richpresence.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class I18NToggleCommand extends ClientOnlyDeveloperCommand {
    private ModMain mod = ModMain.getInstance();
    private Configuration configuration = mod.getConfiguration();

    @Override
    public String getCommandBase() {
        return "richpresence_i18n_toggle";
    }

    @Override
    public void processCommand(ICommandSender p_71515_1_, String[] p_71515_2_) {
        boolean i18nEnabled = mod.isI18nEnabled();
        i18nEnabled = !i18nEnabled;
        mod.setI18nEnabled(i18nEnabled);

        configuration.getCategory(Configuration.CATEGORY_GENERAL).put("i18nenabled", new Property("i18nenabled", i18nEnabled ? "true" : "false", Property.Type.BOOLEAN));
        configuration.save();

        String gameLanguage = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        mod.applyLocale(gameLanguage, true);

        sendChatMessage(p_71515_1_, "Changed i18n status to: §6" + i18nEnabled + "§e!");
    }
}
