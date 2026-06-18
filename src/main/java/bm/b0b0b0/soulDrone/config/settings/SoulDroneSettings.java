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

    @Comment(@CommentValue("Стоимость отправки через Vault. 0 = бесплатно. Нет Economy на сервере — тоже бесплатно (см. консоль при старте)"))
    public double sendCost = 50.0;

    @NewLine
    @Comment(@CommentValue("SQLite файл (относительно папки плагина)"))
    public String sqliteFile = "soulDrone.db";

    @Comment(@CommentValue("HikariCP pool size"))
    public int databasePoolSize = 10;

    @Comment(@CommentValue("Хранить посылки в БД (сутки). После — удаление"))
    public double packageStorageDays = 10.0;

    @Comment(@CommentValue("Разрешить /send оффлайн игрокам"))
    public boolean allowOfflineSend = true;

    @Comment(@CommentValue("Оффлайн получатель: не ждать accept, сразу груз"))
    public boolean autoAcceptOffline = true;

    @Comment(@CommentValue("Подкоманда забрать посылку из БД"))
    public String claimSubcommand = "claim";

    @Comment(@CommentValue("Подкоманда отказаться от посылки в БД"))
    public String refuseSubcommand = "refuse";

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

    @Comment(@CommentValue("Ожидание после частичного забора (секунды)"))
    public double partialPickupWaitSeconds = 120.0;

    @NewLine
    @Comment(@CommentValue("Дистанция спавна от отправителя (блоки)"))
    public double spawnDistance = 2.5;

    @Comment(@CommentValue("Доп. высота над головой игрока при следовании (блоки)"))
    public double spawnHeight = 0.55;

    @Comment(@CommentValue("Смещение сбоку при следовании за отправителем (блоки), 0 = строго спереди"))
    public double followSideOffset = 0.6;

    @Comment(@CommentValue("Мёртвая зона: дрон не двигается, пока цель ближе (гориз., блоки)"))
    public double followIdleRadius = 2.0;

    @Comment(@CommentValue("Макс. скорость догона за отправителем (блоков/тик)"))
    public double followMaxSpeed = 0.085;

    @Comment(@CommentValue("Поворот «плеча» дрона только если игрок развернулся больше (градусы)"))
    public double followYawThreshold = 45.0;

    @Comment(@CommentValue("Плавность поворота плеча дрона за игроком (0.05–1)"))
    public double followYawLerp = 0.05;

    @Comment(@CommentValue("Устаревшее: не используется, см. followMaxSpeed"))
    public double followLerp = 0.06;

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
    public double bobAmplitude = 0.035;

    @Comment(@CommentValue("Скорость покачивания (множитель sin)"))
    public double bobSpeed = 0.065;

    @Comment(@CommentValue("Амплитуда бокового покачивания"))
    public double swayAmplitude = 0.035;

    @Comment(@CommentValue("Скорость бокового покачивания"))
    public double swaySpeed = 0.075;

    @Comment(@CommentValue("Скорость вращения при вылете (градусы/тик)"))
    public double departureSpinSpeed = 18.0;

    @Comment(@CommentValue("Масштаб модели дрона (0.25 = маленький, 1.0 = размер блока)"))
    public float blockScale = 0.42f;

    @Comment(@CommentValue("Shift+ПКМ по дрону — предпросмотр груза без забора/редактирования"))
    public boolean cargoPreviewOnSneak = true;

    @Comment(@CommentValue("Партиклы: дым с вентиляторов и сверху"))
    public boolean droneParticlesEnabled = true;

    @Comment(@CommentValue("Звук моторов рядом с дроном"))
    public boolean droneSoundsEnabled = true;

    @Comment(@CommentValue("Интервал loop-звука (тики)"))
    public int droneSoundLoopIntervalTicks = 5;

    @Comment(@CommentValue("Loop: ENTITY_BEE_LOOP, minecraft:entity.bee.loop или ключ из resource pack"))
    public DroneSoundEntry droneSoundLoop = new DroneSoundEntry("ENTITY_BEE_LOOP", 0.65f, 1.12f);

    @Comment(@CommentValue("Сборка завершена"))
    public DroneSoundEntry droneSoundAssemblyReady = new DroneSoundEntry("BLOCK_AMETHYST_BLOCK_CHIME", 0.8f, 1.2f);

    @Comment(@CommentValue("Прилёт завершён"))
    public DroneSoundEntry droneSoundArrivalReady = new DroneSoundEntry("BLOCK_AMETHYST_BLOCK_CHIME", 0.9f, 0.9f);

    @Comment(@CommentValue("Улёт — первый звук"))
    public DroneSoundEntry droneSoundDeparturePrimary = new DroneSoundEntry("BLOCK_BEACON_DEACTIVATE", 0.55f, 1.35f);

    @Comment(@CommentValue("Улёт — второй звук"))
    public DroneSoundEntry droneSoundDepartureSecondary = new DroneSoundEntry("ENTITY_ENDERMAN_TELEPORT", 0.45f, 0.85f);

    @NewLine
    @Comment(@CommentValue("Ответка за удар по дрону"))
    public boolean dronePunchEnabled = true;

    @Comment(@CommentValue("Урон ответки (1.0 = полсердца)"))
    public double dronePunchDamage = 1.0;

    @Comment(@CommentValue("Отбрасывание при ответке"))
    public double dronePunchKnockback = 0.42;

    @Comment(@CommentValue("Кулдаун ответки на игрока (тики)"))
    public int dronePunchCooldownTicks = 14;

    @Comment(@CommentValue("Радиус, в котором слышны фразы дрона (блоки)"))
    public double dronePunchBroadcastRadius = 18.0;

    @Comment(@CommentValue("Дистанция удара по дрону (блоки)"))
    public double dronePunchReach = 3.8;

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
    @Comment(@CommentValue("Миры, где дрон не появляется (имя мира Bukkit)"))
    public List<String> blockedWorlds = new ArrayList<>();

    @Comment(@CommentValue("Регионы WorldGuard: world:region_id или *:region_id для любого мира"))
    public List<String> blockedRegions = new ArrayList<>();

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

    @Comment(@CommentValue("Permission: игнорировать запрещённые миры/регионы"))
    public String bypassZonesPermission = "soulDrone.bypass-zones";

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

    public static final class DroneSoundEntry {
        public String sound = "ENTITY_BEE_LOOP";
        public float volume = 0.65f;
        public float pitch = 1.12f;

        public DroneSoundEntry() {
        }

        public DroneSoundEntry(String sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
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
