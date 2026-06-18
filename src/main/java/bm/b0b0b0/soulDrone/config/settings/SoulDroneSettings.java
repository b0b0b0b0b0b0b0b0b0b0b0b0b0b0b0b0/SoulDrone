package bm.b0b0b0.soulDrone.config.settings;

import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public class SoulDroneSettings extends YamlSerializable {

    private static final SerializerConfig CONFIG = new SerializerConfig.Builder().build();

    @Comment(@CommentValue("Стоимость отправки через Vault. 0 = бесплатно"))
    public double sendCost = 50.0;

    @Comment(@CommentValue("Требовать Vault для отправки"))
    public boolean requireVault = false;

    @NewLine
    @Comment(@CommentValue("Получатель должен принять запрос (/send accept)"))
    public boolean requireReceiverAccept = true;

    @Comment(@CommentValue("Время на accept/deny (секунды)"))
    public double requestWaitSeconds = 120.0;

    @NewLine
    @Comment(@CommentValue("Ожидание отправителя у дрона (секунды)"))
    public double senderWaitSeconds = 240.0;

    @Comment(@CommentValue("Ожидание получателя у дрона (секунды)"))
    public double receiverWaitSeconds = 1800.0;

    @NewLine
    @Comment(@CommentValue("Дистанция спавна от отправителя (блоки)"))
    public double spawnDistance = 2.5;

    @Comment(@CommentValue("Высота спавна над землёй (блоки)"))
    public double spawnHeight = 1.2;

    @Comment(@CommentValue("Дистанция посадки перед получателем (блоки)"))
    public double landingDistance = 2.5;

    @Comment(@CommentValue("Высота появления над получателем (блоки)"))
    public double arrivalHeight = 6.0;

    @Comment(@CommentValue("Скорость подъёма при вылете (блоков/тик)"))
    public double departureRiseSpeed = 0.18;

    @Comment(@CommentValue("Скорость снижения при прилёте (блоков/тик)"))
    public double arrivalDescentSpeed = 0.14;

    @NewLine
    @Comment(@CommentValue("Длительность сборки (тики)"))
    public int assemblyDurationTicks = 40;

    @Comment(@CommentValue("Длительность вылета (тики)"))
    public int departureDurationTicks = 55;

    @Comment(@CommentValue("Длительность прилёта (тики)"))
    public int arrivalDurationTicks = 70;

    @Comment(@CommentValue("Амплитуда вертикального покачивания в ожидании"))
    public double bobAmplitude = 0.08;

    @Comment(@CommentValue("Скорость покачивания (множитель sin)"))
    public double bobSpeed = 0.11;

    @Comment(@CommentValue("Амплитуда бокового покачивания"))
    public double swayAmplitude = 0.06;

    @Comment(@CommentValue("Скорость бокового покачивания"))
    public double swaySpeed = 0.075;

    @Comment(@CommentValue("Скорость вращения при вылете (градусы/тик)"))
    public double departureSpinSpeed = 18.0;

    @Comment(@CommentValue("Масштаб модели дрона (0.25 = маленький, 1.0 = размер блока)"))
    public float blockScale = 0.42f;

    @NewLine
    @Comment(@CommentValue("Hex-акцент для TextDisplay (без #)"))
    public String accentHex = "AA00FF";

    @Comment(@CommentValue("Смещение TextDisplay по Y от якоря"))
    public float labelOffsetY = 0.72f;

    @Comment(@CommentValue("Фон TextDisplay (ARGB, 0 = без фона)"))
    public int labelBackgroundArgb = 0x66000000;

    @Comment(@CommentValue("Показывать TextDisplay над дроном"))
    public boolean labelEnabled = true;

    @Comment(@CommentValue("Ширина кликабельной зоны (Interaction entity)"))
    public float hitboxWidth = 0.95f;

    @Comment(@CommentValue("Высота кликабельной зоны (Interaction entity)"))
    public float hitboxHeight = 0.8f;

    @Comment(@CommentValue("Запасной радиус ПКМ, если клик мимо hitbox (блоки)"))
    public double clickRadius = 4.0;

    @NewLine
    @Comment(@CommentValue("Сегменты модели: forward, lateral, vertical, material"))
    public List<DroneSegmentEntry> segments = defaultSegments();

    @NewLine
    @Comment(@CommentValue("Permission: /send"))
    public String sendPermission = "soulDrone.send";

    @Comment(@CommentValue("Permission: /send accept"))
    public String acceptPermission = "soulDrone.accept";

    @Comment(@CommentValue("Permission: /send deny"))
    public String denyPermission = "soulDrone.deny";

    @Comment(@CommentValue("Подкоманда принятия"))
    public String acceptSubcommand = "accept";

    @Comment(@CommentValue("Подкоманда отклонения"))
    public String denySubcommand = "deny";

    @Comment(@CommentValue("Подкоманда вкл/выкл приём посылок"))
    public String toggleSubcommand = "toggle";

    @Comment(@CommentValue("По умолчанию игрок принимает посылки"))
    public boolean defaultReceivesDrones = true;

    @Comment(@CommentValue("Permission: /send toggle"))
    public String togglePermission = "soulDrone.toggle";

    @Comment(@CommentValue("Permission: открыть грузовое меню у отправителя"))
    public String openPermission = "soulDrone.open";

    @Comment(@CommentValue("Permission: забрать посылку у получателя"))
    public String receivePermission = "soulDrone.receive";

    @Comment(@CommentValue("Permission: без оплаты Vault"))
    public String bypassCostPermission = "soulDrone.bypass-cost";

    @Comment(@CommentValue("Локаль сообщений: ru или en"))
    public String language = "ru";

    public SoulDroneSettings() {
        super(CONFIG);
    }

    public static List<DroneSegmentEntry> defaultSegments() {
        List<DroneSegmentEntry> list = new ArrayList<>();
        list.add(new DroneSegmentEntry(0, 0, 0, "PURPLE_CONCRETE"));
        list.add(new DroneSegmentEntry(1, 1, 0, "PURPUR_BLOCK"));
        list.add(new DroneSegmentEntry(-1, 1, 0, "PURPUR_BLOCK"));
        list.add(new DroneSegmentEntry(1, -1, 0, "PURPUR_BLOCK"));
        list.add(new DroneSegmentEntry(-1, -1, 0, "PURPUR_BLOCK"));
        list.add(new DroneSegmentEntry(0, 0, 1, "SHULKER_BOX"));
        return list;
    }

    public static final class DroneSegmentEntry {
        public int forward;
        public int lateral;
        public int vertical;
        public String material;

        public DroneSegmentEntry() {
        }

        public DroneSegmentEntry(int forward, int lateral, int vertical, String material) {
            this.forward = forward;
            this.lateral = lateral;
            this.vertical = vertical;
            this.material = material;
        }
    }

}
