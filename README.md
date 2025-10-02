# ChangeMyHeight

<br>

**ChangeMyHeight** — плагин для Minecraft, который добавляет настраиваемые зелья Атрибутов.<br>
Полностью поддерживает *Folia* и работает на Paper.

<br>

## ⚡ Основное

<br>

* Простой в использовании.
* Для моментального сброса всех эффектов, игрок может выпить молоко.
* Если выпить 2 разных зелья, те атрибуты что есть у обоих зелий будут заменены на значения последнего зелья.
* Можно выпить хоть все зелья, они не отменяют друг-друга.
* Полностью настраиваемые зелья.
* Простое управление через команды и конфиг.
* Подходит для мини-игр, ролевых сценариев и визуальных эффектов.

<br>

## 🕹 Использование

<br>

* Эффект применяется сразу при использовании зелья.
* Permission `cmh.give` — выдача эффекта игроку, обновить конфигурацию плагина.
* Permission `cmh.use` — для использования рядовых команд `/cmh check` .
* Permission `cmh.list` — для просмотра всех доступных зелий `/cmh list`.

<br>

## 📜 Команды плагина

| Команда                     | Описание                                      | Permission |
|-----------------------------|-----------------------------------------------|------------|
| `/cmh`                      | Основная команда плагина (показывает справку) | `cmh.use`  |
| `/cmh give <зелье> <игрок>` | Выдать зелье игроку                           | `cmh.give` |
| `/cmh check`                | Проверить активные зелья                      | `cmh.use`  |
| `/cmh list`                 | Список всех доступных зелий                   | `cmh.list` |
| `/cmh reload`               | Перезагрузить конфиг плагина                  | `cmh.give` |

<br>

## 💡 Идеи применения

<br>

* Выдача во время РП событий.
* Включение в донатные группы.
* Продажа за внутриигровую валюту.

## 📄 Настройка конфига

* **Полный список атрибутов [тут](https://minecraft.wiki/w/Attribute)**
* **Полный набор партиклов [тут](https://minecraft.wiki/w/Particles_(Java_Edition))**

Стандартный конфиг с пояснениями:

```
potions: #Список зелий
  shrink: #Название зелья
    title: 'Крошик' #То что будет видно игрокам, название с учетом языка сервера
    attributes: #Список атрибутов, атрибуты можно посмотреть тут 'https://minecraft.wiki/w/Attribute'
      "minecraft:scale": 0.5 #Атрибут, который меняет размер игрока, по умолчанию 1
      "minecraft:step_height": 0.2 #Атрибут высоты авто подъема на блоки, по умолчанию 0.7
      "minecraft:movement_speed": 0.1 # Атрибут скорости игрока, делает быстрее на 0.1
    color: '#bfff00' #Цвет бутылочки зелья и его отображения в чате
    duration: 40 #Длительность зелья, в секундах
    description: "Уменьшает в 2 раза, это плохо?" #Описание зелья
    particleType: "firework" #Частицы при использовании, список частиц можно посмотреть тут 'https://minecraft.wiki/w/Particles_(Java_Edition)'
  normal:
    title: 'Гравитрон'
    attributes:
      "minecraft:gravity": 0.02
      "minecraft:safe_fall_distance": 6
    color: '#00ffff'
    duration: 120
    description: "Гравитация теперь добрее к вам"
    particleType: "white_smoke"
  giant:
    title: 'Гигантизм'
    attributes:
      "minecraft:scale": 1.5
      "minecraft:step_height": 1.6
      "minecraft:movement_speed": 0.07
    color: '#ff0000'
    duration: 50
    description: "Увеличивает размер в 2 раза, вам тяжело двигаться?"
    particleType: "composter"
  abyss:
    title: 'Пучина'
    attributes:
      "minecraft:oxygen_bonus": 5
      "minecraft:submerged_mining_speed": 1.5
      "minecraft:water_movement_efficiency": 0.9
      "minecraft:step_height": 0.1
      "minecraft:safe_fall_distance": 1
    color: '#26fe1b'
    duration: 80
    description: "В воде вам становится гораздо лучше"
    particleType: "sculk_charge_pop"

```


