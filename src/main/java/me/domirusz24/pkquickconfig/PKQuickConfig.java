package me.domirusz24.pkquickconfig;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.PassiveAbility;
import com.projectkorra.projectkorra.configuration.Config;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public final class PKQuickConfig {

    @SafeVarargs
    public static<T extends CoreAbility> void addDefaults(Class<T>... clazz) {
        for (Class<T> tClass : clazz) {
            addDefaults(tClass);
        }
    }

    public static<T extends CoreAbility> void addDefaults(Class<T> clazz) {
        if (clazz.isInterface() || clazz.isEnum() || Modifier.isAbstract(clazz.getModifiers())) return;

        Config config = ConfigManager.defaultConfig;
        Config language = ConfigManager.languageConfig;

        try {
            T obj = clazz.getDeclaredConstructor(Player.class).newInstance((Object) null);

            String elementName;
            if (obj.getElement() instanceof Element.SubElement) {
                elementName = ((Element.SubElement)obj.getElement()).getParentElement().getName();
            } else {
                elementName = obj.getElement().getName();
            }

            String name = obj.getName();

            String prefix = "Abilities." + elementName + "." + name + ".";

            if (obj instanceof PassiveAbility) {
                config.get().addDefault("Abilities." + elementName + ".Passive." + name + ".Enabled", true);
            } else {
                config.get().addDefault("Abilities." + elementName + "." + name + ".Enabled", true);
            }

            for (Field declaredField : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(declaredField.getModifiers()) || Modifier.isFinal(declaredField.getModifiers()))
                    continue;

                declaredField.setAccessible(true);

                if (declaredField.isAnnotationPresent(ConfigValue.class)) {
                    String path = prefix + declaredField.getAnnotation(ConfigValue.class).value();
                    config.get().addDefault(path, declaredField.get(obj));
                } else if (declaredField.isAnnotationPresent(AbilityCooldown.class)) {
                    config.get().addDefault(prefix + "Cooldown", declaredField.get(obj));
                } else if (declaredField.isAnnotationPresent(LanguageValue.class)) {
                    String path = prefix + declaredField.getAnnotation(LanguageValue.class).value();
                    language.get().addDefault(path, declaredField.get(obj));
                } else if (declaredField.isAnnotationPresent(AbilityDescription.class)) {
                    if (obj instanceof PassiveAbility) {
                        language.get().addDefault("Abilities." + elementName + ".Passive." + name + ".Description", declaredField.get(obj));
                    } else if (obj instanceof ComboAbility) {
                        language.get().addDefault("Abilities." + elementName + ".Combo." + name + ".Description", declaredField.get(obj));
                    } else {
                        language.get().addDefault("Abilities." + elementName + "." + name + ".Description", declaredField.get(obj));
                    }
                } else if (declaredField.isAnnotationPresent(AbilityInstruction.class)) {
                    if (obj instanceof ComboAbility) {
                        language.get().addDefault("Abilities." + elementName + ".Combo." + name + ".Instructions", declaredField.get(obj));
                    } else {
                        language.get().addDefault("Abilities." + elementName + "." + name + ".Instructions", declaredField.get(obj));
                    }
                }
            }

            config.save();
            language.save();

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static<T extends CoreAbility> void load(T obj, Class<T> clazz) {
        if (obj.getPlayer() == null || !obj.isEnabled()) return;

        String elementName;
        if (obj.getElement() instanceof Element.SubElement) {
            elementName = ((Element.SubElement)obj.getElement()).getParentElement().getName();
        } else {
            elementName = obj.getElement().getName();
        }

        String name = obj.getName();

        String prefix = "Abilities." + elementName + "." + name + ".";

        Config config = ConfigManager.defaultConfig;

        for (Field declaredField : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(declaredField.getModifiers()) || Modifier.isFinal(declaredField.getModifiers())) continue;
            declaredField.setAccessible(true);
            if (declaredField.isAnnotationPresent(ConfigValue.class)) {
                String path = prefix + declaredField.getAnnotation(ConfigValue.class).value();
                try {
                    Object defaultValue = declaredField.get(obj);
                    declaredField.set(obj, config.get().get(path, defaultValue));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
