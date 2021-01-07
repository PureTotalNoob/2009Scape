package core.game.interaction

import core.cache.Cache
import core.cache.def.impl.ItemDefinition
import core.cache.def.impl.NPCDefinition
import core.cache.misc.buffer.ByteBufferUtils
import core.game.node.entity.player.Player
import plugin.skill.Skills
import plugin.stringtools.colorize
import java.nio.ByteBuffer


/**
 * Handles the sending of quick chat messages and string-replacement for specific messages.
 * @author Ceikry
 */
//TODO: A lot of the menus defined in the massive arrays at the bottom need to be rearranged because the order they appear in the menu and the order they are actually in is quite different. Start clicking through the "can you cook me..." menu to see what I mean.
//TODO: I've done the groundwork, Please someone else take up the task of rearranging these menus
object QCRepository {

    val skillIDs = hashMapOf(
            0 to "Agility",
            1 to "Attack",
            2 to "Construction",
            3 to "Cooking",
            4 to "Crafting",
            5 to "Defence",
            6 to "Farming",
            7 to "Firemaking",
            8 to "Fishing",
            9 to "Fletching",
            10 to "Herblore",
            11 to "Hitpoints",
            12 to "Hunter",
            13 to "Thieving",
            14 to "Summoning",
            15 to "Strength",
            16 to "Smithing",
            19 to "Slayer",
            20 to "Runecrafting",
            21 to "Ranged",
            22 to "Prayer",
            23 to "Mining",
            24 to "Magic",
            25 to "Woodcutting"
    )

    private val quickChatIndex = Cache.getIndexes()[24]
    private val qcSize = quickChatIndex.getFilesSize(1)

    @JvmStatic
    fun sendQC(player: Player?, type: Int?, id: Int?, searchId: Int? = -1) {
        id ?: return
        type ?: return

        var index = if(id > 0) {
            (256 * type) + id
        }else {
            if(type == 0) 256 + id else 512 + id
        } //for some reason it subtracts only from 512 if it's negative (Jagex idk) or 256 if type is 0 but not from 768 if type is 3?

        var qcString = ByteBufferUtils.getString(ByteBuffer.wrap(quickChatIndex.getFileData(1, index)))

        //XP to next level
        if (qcString.contains("to get my next")) {
            val skillName = skillIDs[id - 103]
            val skill = Skills.getSkillByName(skillName?.toUpperCase())
            val playerXP = player?.skills?.getExperience(skill)
            val playerLevel = player?.skills?.getStaticLevel(skill)
            val nextXP = player?.skills?.getExperienceByLevel(playerLevel?.plus(1) ?: 1)
            qcString = qcString.replace("<", "${(nextXP?.minus(playerXP ?: 0.0) ?: 0.0).toInt()}")
        }

        //My X level is
        else if (qcString.contains("level is")) {
            val skillName = qcString.split(" ")[1]
            val skill = Skills.getSkillByName(skillName.toUpperCase())
            val level = player?.skills?.getStaticLevel(skill)
            qcString = qcString.replace("<", level.toString())
        }

        //Messages that include items
        else if ((qcString.contains("item") || qcString.contains("Can I buy your") || qcString.contains("What is the best world to buy") || qcString.contains("What is the best world to sell") || qcString.contains("Would you like to borrow") || qcString.contains("Could I please borrow")  ) && qcString.contains("<") ){
            val itemName = ItemDefinition.forId(searchId ?: 0).name
            qcString = qcString.replace("<",itemName)
        }

        //Loan duration
        else if (qcString.contains("I'd like the loan duration to be")){
           qcString = when(searchId){
               0 -> qcString.replace("<","just until one of us logs out")
               else -> qcString.replace("<","${searchId} hours")
           }
        }

        //Go to agility course / Try training your agility at
        else if (qcString.contains("Let's go to Agility course:") || qcString.contains("Try training your Agility at:")){
            qcString = qcString.replace("<", AGILITY_COURSES[searchId ?: 0])
        }

        //Try training on
        else if(qcString.contains("Try training on")){
            qcString = qcString.replace("<", TRAIN_MELEE_NPCS[searchId ?: 0])
        }

        //Try ranging
        else if(qcString.contains("Try ranging")){
            qcString = qcString.replace("<", TRAIN_RANGE_NPCS[searchId ?: 0])
        }

        //flat-pack
        else if(qcString.contains("flat-pack")){
            qcString = qcString.replace("<", FLATPAKS[searchId ?: 0])
        }

        //Cooking
        else if(qcString.contains("I'm cooking") || qcString.contains("Would you please cook me") || qcString.contains("Try cooking")){
            qcString = qcString.replace("<", COOK_ITEMS[searchId ?: 0])
        }

        //Crafting
        else if(qcString.contains("I am crafting") || qcString.contains("Try crafting") || qcString.contains("Would you please craft me")){
            qcString = qcString.replace("<", CRAFT_ITEMS[searchId ?: 0])
        }

        //Farming
        else if(qcString.contains("I'm growing crop") || qcString.contains("Try growing crop")){
            qcString = qcString.replace("<", FARM_CROPS[searchId ?: 0])
        }

        //I'm burning logs...
        else if(qcString.contains("I'm burning logs")){
            qcString = qcString.replace("<", FM_LOGS[searchId ?: 0])
        }

        //Try burning logs at
        else if(qcString.contains("Try burning logs at")){
            qcString = qcString.replace("<", FM_LOCATIONS[searchId ?: 0])
        }

        //Fishing
        else if(qcString.contains("I'm fishing") || qcString.contains("Would you please fish me") || qcString.contains("Try fishing for")){
            qcString = qcString.replace("<", FISH[searchId ?: 0])
        }

       //Fletching
       else if(qcString.contains("I'm fletching") || qcString.contains("Try fletching") || qcString.contains("Would you please fetch me")){
           qcString = qcString.replace("<", FLETCH_ITEMS[searchId ?: 0])
        }

        //Herblore
        else if(qcString.contains("I'm mixing potion") || qcString.contains("Would you please mix me a potion") || qcString.contains("Try mixing potion")){
            qcString = qcString.replace("<", HERB_POTIONS[searchId ?: 0])
        }

        //Herblore secondaries
        else if(qcString.contains("Where can I get the secondary ingredient")){
            qcString = qcString.replace("<", HERB_SECONDARIES[searchId ?: 0])
        }

        //Hunter
        else if(qcString.contains("Would you please hunt me") || qcString.contains("Try hunting")){
            qcString = qcString.replace("<", HUNTER_ANIMALS[searchId ?: 0])
        }

        //Casting spell
        else if(qcString.contains("I'm casting spell") || qcString.contains("Would you please cast")){
            qcString = qcString.replace("<", MAGIC_SPELLS[searchId ?: 0])
        }

        //Spellbook
        else if(qcString.contains("I am on spell book")){
            qcString = qcString.replace("<", MAGIC_BOOKS[searchId ?: 0])
        }

        //Mining ore
        else if(qcString.contains("I'm mining ore")){
            qcString = qcString.replace("<", ORES[searchId ?: 0])
        }

        //Pickaxe using
        else if(qcString.contains("I'm using a pick")){
            qcString = qcString.replace("<", PICKAXE_TYPES[searchId ?: 0])
        }

        //Try mining at
        else if(qcString.contains("Try mining at")){
            qcString = qcString.replace("<", MINING_LOCATIONS[searchId ?: 0])
        }

        //Use your prayer
        else if(qcString.contains("Use your prayer")){
            qcString = qcString.replace("<", PRAYERS[searchId ?: 0])
        }

        //i'm going to craft rune
        else if(qcString.contains("I'm going to craft rune")){
            qcString = qcString.replace("<", RUNES[searchId ?: 0])
        }

        //try crafting runes at
        else if(qcString.contains("Try crafting runes at")){
            qcString = qcString.replace("<", RC_LOCATIONS[searchId ?: 0])
        }

        //Slayer master in
        else if(qcString.contains("You should use the Slayer master in")){
            qcString = qcString.replace("<", SLAYER_LOCATIONS[searchId ?: 0])
        }

        //My current slayer assignment is
        else if(qcString.contains("My current Slayer assignment is")){
            val amount = player?.slayer?.amount ?: 0
            val taskName = NPCDefinition.forId(player?.slayer?.task?.ids?.get(0) ?: 0).name.toLowerCase()
            if(amount > 0){
                qcString = qcString.replace("complete","$amount $taskName")
            }
        }

        //Spare slayer equipment
        else if(qcString.contains("Do you have spare Slayer equipment")){
            qcString = qcString.replace("<", SLAYER_EQUIPMENT[searchId ?: 0])
        }

        //Smithing TODO: Implement Smithing menu (I got tired of this shit -Ceikry)
        else if(qcString.contains("I am smithing") || qcString.contains("Try smithing") || qcString.contains("Would you please smith me")){
            player?.sendMessage(colorize("%RThis Quick Chat is not yet implemented."))
            return
        }

        //familiars
        else if(qcString.contains("I like the familiar") || qcString.contains("I can summon up to")){
            qcString = qcString.replace("<", FAMILIARS[searchId ?: 0])
        }

        //charm droppers
        else if(qcString.contains("Good charm droppers are")){
            qcString = qcString.replace("<", CHARM_DROPPERS[searchId ?: 0])
        }

        //Thieving
        else if(qcString.contains("Try thieving from")){
            qcString = qcString.replace("<", THIEVE_TARGETS[searchId ?: 0])
        }

        //Axe made of
        else if(qcString.contains("I'm using a woodcutting axe made")){
            qcString = qcString.replace("<", AXE_TYPES[searchId ?: 0])
        }

        //Try training woodcutting at
        else if(qcString.contains("Try training Woodcutting at")){
            qcString = qcString.replace("<", WOODCUTTING_LOCATIONS[searchId ?: 0])
        }

        player?.sendChat(qcString)
    }
}

private val AGILITY_COURSES: Array<String> = arrayOf("Agility Pyramid","Ape Atoll","Barbarian Outpost","Brimhaven Agility Arena","Dorgesh-Kaan","Gnome Stronghold Course","Penguin Course","Werewolf Agility Course","Wilderness Course","Gnome Ball","Werewolf Skullball")
private val TRAIN_MELEE_NPCS: Array<String> = arrayOf("black demons","blue dragons","chickens","cows","cyclopes","dagannoths","experiments","fire giants","goblins","guards","hill giants","ice giants","lesser demons","moss giants","Slayer creatures")
private val TRAIN_RANGE_NPCS: Array<String> = arrayOf("barbarians","black demons","blue dragons","chickens","cows","dark wizards","dwarves","fire giants","goblins","guards","hill giants","lesser demons","moss giants","rats","zombies")
private val FLATPAKS: Array<String> = arrayOf("Asgarnian Ale barrel","beer barrel","carved oak bench","carved oak dining table","carved oak magic wardrobe","carved teak bench","carved teak magic wardrobe","carved teak table","Chef's Delight barrel","cider barrel","crude wooden chair","Dragon Bitter barrel","fancy teak dresser", "four-poster bed","gilded bench","gilded cape rack","gilded clock","gilded dresser","gilded four-poster bed","gilded magic wardrobe","gilded wardrobe","Greenman's Ale barrel","large oak bed","large teak bed","magical cape rack","mahogany armchair","mahogany armour case","mahogany bench","mahogany bookcase","mahogany cape rack","mahogany costume box","mahogany dresser","mahogany magic wardrobe","mahogany table","mahogany toy box","mahogany treasure chest","mahogany wardrobe","marble cape rack","marble magic wardrobe","oak armchair","oak armour case","oak bed","oak bench","oak bookcase","oak cape rack","oak chair","oak clock","oak costume box","oak dining table","oak drawers","oak dresser","oak kitchen table","oak magic wardrobe","oak shaving stand","oak toy box","oak treasure chest","oak wardrobe","opulent table","rocking chair","shaving stand","shoe box","teak armchair","teak armour case","teak bed","teak cape rack","teak clock","teak costume box","teak dining bench","teak drawers","teak dresser","teak kitchen table","teak magic wardrobe","teak table","teak toy box","teak treasure chest","teak wardrobe","wooden bed","wooden bench","wooden bookcase","wooden chair","wooden dining table","wooden kitchen table")
private val COOK_ITEMS: Array<String> = arrayOf("admiral pie","anchovies","anchovy pizza","apple pie","bass","bear meat","bread","bird meat","cake","cave eel","cheese and tomato batta","chicken","chilli con carne","chilli potato","choc chip crunchies","chocolate bomb","chocolate cake","chompy","cod","cooked pawya","cooked rabbit","crab meat","crayfish","curry","egg and tomato","egg and tomato potato","fat snail","fish pie","fishcake","fried mushrooms","fried onions","fruit batta","garden pie","herring","jubbly","karambwan","karambwanji","lava eel","lean snail","leaping salmon","leaping sturgeon","leaping trout","lobster","mackerel","manta ray","meat","meat pie","meat pizza","monkfish","mud pie","mushroom and onion","mushroom and onion potato","oomlie wrap","pawya","pike","pineapple pizza","pitta bread","plain pizza","potato","rabbit","rainbow fish","rat meat","redberry pie","roasted beast meat","roasted rabbit","baked potato with butter","salmon","sardine","baked potato","scrambled egg","sea turtle","seaweed","shark","shrimp","slimy eel","spice crunchies","spicy meat","spicy sauce","spider on a stick","stew","summer pie","sweetcorn","swordfish","tangled toads' legs","thin snail","toad batta","toad crunchies","trout","tuna","tuna and corn","tuna and corn potato","ugthanki kebab","ugthanki meat","vegball","vegetable ball","vegetable batta","wild pie","worm batta","worm crunchies","worm hole")
private val CRAFT_ITEMS: Array<String> = arrayOf("air battlestaves","balls of wool","baskets","beer glasses","black dragonhide bodies","black dragonhide chaps","black dragonhide vambraces","black spiked vambraces","blue dragonhide bodies","blue dragonhide chaps","blue dragonhide vambraces","blue spiked vambraces","bowls","bowstrings","bullseye lanterns","candle lanterns","cloth","coifs","crossbow strings","diamond amulets","diamond bracelets","diamond necklaces","diamond rings","diamonds","Dorgeshuun light orbs","dragonstone amulets","dragonstone bracelets","dragonstone necklaces","dragonstone rings","dragonstones","earth battlestaves","emerald amulets","emerald bracelets","emerald necklaces","emerald rings","emeralds","feather headdresses","fire battlestaves","fishbowls","fremennik round shields","glass orbs","gold amulets","gold bracelets","gold necklaces","gold rings","green dragonhide bodies","green dragonhide chaps","green dragonhide vambraces","green spiked vambraces","hard leather bodies","holy symbols","leather bodies","leather boots","leather chaps","leather cowls","leather gloves","leather vambraces","lightning conductors","limestone bricks","magic string","oil lamps","oil lanterns","onyx","onyx amulets","onyx bracelets","onyx necklaces","onyx rings","pie dishes","plant pots","pot lids","pots","red dragonhide bodies","red dragonhide chaps","red dragonhide vambraces","red spiked vambraces","rope","rubies","ruby amulets","ruby bracelets","ruby necklaces","ruby rings","sacks","sapphire amulets","sapphire bracelets","sapphire necklaces","sapphire rings","sapphires","silver bolts","silver sickles","snakeskin bandannas","snakeskin bodies","snakeskin boots","snakeskin chaps","snakeskin vambraces","snelms","spiked vambraces","studded bodies","studded leather chaps","tiaras","unholy symbols","unpowered orbs","vials","water battlestaves","yak-hide bodies","yak-hide legs")
private val FARM_CROPS: Array<String> = arrayOf("apples","Asgarnian hops","avantoe","bananas","barley","bittercap mushrooms","cabbages","cacti","cadantine","cadavaberries","calquats","coconuts","curry leaves","deadly nightshade","dwarf weed","dwellberries","evil turnips","goutweed","guam","hammerstone hops","harralander","irit","jade vine","jangerberries","jute fiber","Krandorian hops","kwuarm","lantadyme","limpwurt roots","magic trees","maple trees","marigolds","marrentill","nasturtium","oak trees","onions","oranges","papayas","pineapples","poison ivy berries","potatoes","ranarr","redberries","rosemary","snapdragon","spirit trees","spirit weed","strawberries","sweetcorn","tarromin","toadflax","tomatoes","torstol","watermelons","whiteberries","wildblood hops","willow trees","woad leaves","Yanillian hops","yew trees")
private val FM_LOGS: Array<String> = arrayOf("achey","arctic pine","eucalyptus","logs","magic","mahogany","maple","oak","teak","willow","yew")
private val FM_LOCATIONS: Array<String> = arrayOf("Draynor Village","Falador east bank","Fishing Colony","Oo'glog","Port Phasmatys","Yanille")
private val FISH: Array<String> = arrayOf("anchovies","bass","caskets","cave eel","cod","crayfish","frog spawn","herring","karambwan","karambwanji","lava eel","leaping salmon","leaping sturgeon","leaping trout","lobster","mackerel","manta ray","monkfish","oysters","pike","rainbow fish","salmon","sardine","sea turtle","seaweed","shark","shrimp","slimy eel","swordfish","trout","tuna")
private val FLETCH_ITEMS: Array<String> = arrayOf("adamant arrows","adamant bolts","adamant brutal arrows","adamant crossbows","adamant darts","arrow shafts","barbed bolts","black brutal arrows","blurite bolts","blurite crossbows","bronze arrows","bronze bolts","bronze brutal arrows","bronze crossbows","bronze darts","composite ogre bows","crossbow stocks","diamond bolts","dragon arrows","dragon darts","dragonstone bolts","emerald bolts","flighted ogre arrows","headless arrows","iron arrows","iron bolts","iron brutal arrows","iron crossbows","iron darts","jade bolts","longbows","magic longbows","magic shortbows","mahogany crossbow stocks","maple crossbow stocks","maple longbows","maple shortbows","mithril arrows","mithril bolts","mithril brutal arrows","mithril crossbows","mithril darts","mithril grapples","oak crossbow stocks","oak longbows","oak shortbows","ogre arrow shafts","ogre arrows","onyx bolts","opal bolts","pearl bolts","red topaz bolts","ruby bolts","rune arrows","rune bolts","rune brutal arrows","rune crossbows","rune darts","sapphire bolts","shortbows","silver bolts","steel arrows","steel bolts","steel brutal arrows","steel crossbows","steel darts","teak crossbow stocks","willow crossbow stocks","willow longbows","willow shortbows","yew crossbow stocks","yew longbows","yew shortbows")
private val HERB_POTIONS: Array<String> = arrayOf("Agility potion","Anti fire-breath potion","Antipoison","Attack potion","Blamish oil","Combat potion","Defence potion","Energy potion","Fishing potion","Guthix Balance","Hunter potion","Magic essence potion","Magic potion","Prayer restore","Ranging potion","Relicym's Balm","Restore potion","Sanfew Serum","Saradomin Brew","Strength potion","Summoning potion","Super antipoison","Super antipoison+","Super antipoison++","Super Attack potion","Super Defence potion"," Super energy potion","Super fishing explosive","Super restore potion","Super Strength potion","Weapon poison","Weapon poison+","Weapon poison++","Zamorak Brew")
private val HERB_SECONDARIES: Array<String> = arrayOf("blamish snail slime","cactus spine","chocolate dust","cockatrice egg","crushed nest","deadly nightshade","eye of newt","garlic","ground blue dragon scale","ground goat's horn","ground gorak claw","ground unicorn horn","jangerberries","kebbit teeth dust","limpwurt root","magic tree roots","Mort Myre fungi","nail beast nails","poison ivy berries","potato cactus","red spiders' eggs","rubium","silver dust","snake weed","snape grass","toad's legs","white berries","wine of Zamorak","yew roots")
private val HUNTER_ANIMALS: Array<String> = arrayOf("baby impling","barbtailed kebbit","black salamander","black warlock","cerulean twitch","chinchompa","common kebbit","copper longtail","crimson swift","dark kebbit","dashing kebbit","desert devil","dragon impling","earth impling","eclectic impling","essence impling","Feldip weasel","ferret","golden warbler","gourmet impling","grenwall","horned graahk","imp","magpie impling","nature impling","ninja impling","orange salamander","pawya","polar kebbit","prickly kebbit","rabbit","razorbacked kebbit","red chinchompa","red salamander","ruby harvest","sabretooth kebbit","sabretooth kyatt","sapphire glacialis","snowy knight","spined larupia","spotted kebbit","swamp lizard","tropical wagtail","wild kebbit","young impling")
private val MAGIC_SPELLS: Array<String> = arrayOf("Annakarl Teleport","Ape Atoll Teleport","Ardougne Teleport","Bake Pie","Barbarian Teleport","Bind","Blood Barrage","Blood Blitz","Blood Burst","Blood Rush","Bones to Bananas","Bones to Peaches","Boost Potion Share","Bounty Locate","Camelot Teleport","Carrallangar Teleport","Catherby Teleport","Charge","Charge Air Orb","Charge Earth Orb","Charge Fire Orb","Charge Water Orb","Claws of Guthix","Confuse","Crumble Undead","Cure Group","Cure Me","Cure Other","Cure Plant","Curse","Dareeyak Teleport","Dream","Earth Blast","Earth Bolt","Earth Strike","Earth Wave","Enchant 1 - Sapphire","Enchant 2 - Emerald","Enchant 3 - Ruby","Enchant 4 - Diamond","Enchant 5 - Dragonstone", "Enchant 6 - Onyx","Enchant Bolt","Energy Transfer","Enfeeble","Entangle","Falador Teleport","Fertilise Soil","Fire Blast","Fire Bolt","Fire Strike","Fire Wave","Fishing Guild Teleport","Flames of Zamorak","Ghorrock Teleport","Heal Group","Heal Other","High Level Alchemy","Home Teleport","House Teleport","Humidify","Hunter Kit","Iban Blast","Ice Barrage","Ice Blitz","Ice Burst","Ice Plateau Teleport","Ice Rush","Kharyll Teleport","Khazard Teleport","Lassar Teleport","Low Level Alchemy","Lumbridge Teleport","Magic Dart","Magic Imbue","Monster Examine","Moonclan Teleport","NPC Contact","Ourania Teleport","Paddewwa Teleport","Plank Make","Saradomin Strike","Senntisten Teleport","Shadow Barrage","Shadow Blitz","Shadow Burst","Shadow Rush","Smoke Barrage","Smoke Blitz","Smoke Burst","Smoke Rush","Snare","Spellbook Swap","Stat Restore Pot Share","Stat Spy","String Jewellery","Stun","Superglass Make","Superheat Item","Tele Group Barbarian","Tele Group Catherby","Tele Group Fishing Guild","Tele Group Ice Plateau","Tele Group Khazard","Tele Group Moonclan","Tele Group Waterbirth","Telegrab","Teleother Camelot","Teleother Falador","Teleother Lumbridge","Trollheim Teleport","Varrock Teleport","Vengeance","Vengeance Other","Vulnerability","Watchtower Teleport","Water Blast","Water Bolt","Water Strike","Water Wave","Waterbirth Teleport","Weaken","Wind Blast","Wind Bolt","Wind Strike","Wind Wave")
private val MAGIC_BOOKS: Array<String> = arrayOf("Standard","Ancients","Lunar")
private val ORES: Array<String> = arrayOf("adamantite","blurite","clay","coal","copper","elemental","gold","granite","iron","limestone","mithril","obsidian","pure essence","rubium","rune essence","runite","sandstone","silver","tin")
private val MINING_LOCATIONS: Array<String> = arrayOf("Ardougne south","Barbarian village","Brimhaven north-west","Brimhaven south-west","Brimhaven volcano gold mine","Coal Trucks","Crafting Guild","Crandor Isle","Dorgeshuun mine","Dwarven Mine","East of Ardougne","Edgeville","Edgeville dungeon mine","Elemental Workshop","Fremennik Isles mine","Grand Tree","Haunted Mine","Heroes' Guild","Ice Caves","Keldagrim","Kharid Scorpion chasm","Lumbridge Caves","Lumbridge swamp","Lunar Isle","Mining Guild","North of Yanille","Piscatoris","Port Khazard","Rimmington","Shilo Village gem mine","TzHaar City","Varrock south-east","Varrock south-west","Wilderness Hobgoblins' mine","Wilderness Lava Maze","Wilderness Pirates' mine","Wilderness runite mine")
private val PRAYERS: Array<String> = arrayOf("Burst of Strength","Chivalry","Clarity of Thought","Eagle Eye","Hawk Eye","Improved Reflexes","Incredible Reflexes","Mystic Lore","Mystic Might","Mystic Will","Piety","Protect from Magic","Protect from Melee","Protect from Missiles","Protect from Summoning","Protect Item","Rapid Heal","Rapid Restore","Redemption","Retribution","Rock Skin","Sharp Eye","Smite","Steel Skin","Superhuman Strength","Thick Skin","Ultimate Strength")
private val RUNES: Array<String> = arrayOf("air runes","astral runes","blood runes","body runes","chaos runes","cosmic runes","death runes","dust runes","earth runes","fire runes","lava runes","law runes","mind runes","mist runes","mud runes","nature runes","smoke runes","soul runes","steam runes","water runes")
private val RC_LOCATIONS: Array<String> = arrayOf("Air Altar","Astral Altar","Blood Altar","Body Altar","Chaos Altar","Cosmic Altar","Death Altar","Earth Altar","Fire Altar","Law Altar","Mind Altar","Nature Altar","Ourania Altar","Water Altar")
private val SLAYER_LOCATIONS: Array<String> = arrayOf("Burthorpe","Canifis","Edgeville Dungeon","Pollnivneach","Shilo Village","Zanaris")
private val FAMILIARS: Array<String> = arrayOf("abyssal lurker","abyssal parasite","abyssal titan","adamant minotaur","albino rat","arctic bear","barker toad","beaver","bloated leech","bronze minotaur","bull ant","bunyip","compost mound","desert wyrm","dreadfowl","evil turnip","fire titan","forge regent","fruit bat","geyser titan","giant chinchompa","giant ent","granite crab","granite lobster","honey badger","hydra","ibis","ice titan","iron minotaur","iron titan","karamthulhu overlord","lava titan","macaw","magpie","mithril minotaur","moss titan","obsidian golem","pack yak","phoenix","praying mantis","pyrelord","ravenous locust","rune minotaur","smoke devil","spirit cobra","spirit cockatrice","spirit coraxatrice","spirit dagannoth","spirit graahk","spirit guthatrice","spirit jelly","spirit kalphite","spirit kyatt","spirit larupia","spirit mosquito","spirit pengatrice","spirit saratrice","spirit scorpion","spirit spider","spirit terrorbird","spirit Tz-Kih","spirit vulatrice","spirit wolf","spirit zamatrice","steel minotaur","steel titan","stranger plant","swamp titan","talon beast","thorny snail","unicorn stallion","vampire bat","void ravager","void shifter","void spinner","void torcher","war tortoise","wolpertinger")
private val CHARM_DROPPERS: Array<String> = arrayOf("basilisks","bloodvelds","dust devils","fire giants","ice giants","ice warriors","jogres","ogres","rock crabs","rock lobsters","waterfiends","wolves")
private val THIEVE_TARGETS: Array<String> = arrayOf("bearded Pollnivnian bandits","cave goblins","chests","desert bandits","elves","farmers","Fremenniks","gnomes","guards","H.A.M. members","heroes","knights","master farmers","men and women","Menaphite thugs","paladins","Pollnivnian bandits","rogues","stalls","warriors","watchmen")
private val PICKAXE_TYPES: Array<String> = arrayOf("adamant","bronze","iron","mithril","rune","steel")
private val AXE_TYPES: Array<String> = arrayOf("adamant","black","bronze","dragon","iron","mithril","rune","steel")
private val WOODCUTTING_LOCATIONS: Array<String> = arrayOf("Ape Atoll","Draynor","Edgeville","Gnome Stronghold","Isafdar","Kharazi Jungle","Mage Training Area","Neitiznot","Piscatoris","Seers' Village","Seers' Village cemetery","Sinclair Mansion","Sorcerer's Tower","South of Castle Wars","South of Falador","South of the Lumberyard","Tai Bwo Bwannai","Uzer","Varrock Palace","West of Barbarian Assault","West of Catherby","West of Lumbridge Castle","West of Oo'glog")
private val SLAYER_EQUIPMENT: Array<String> = arrayOf("a bug lantern","a crystal chime","a face mask","a mirror shield","a nosepeg","a Slayer bell","a spiny helmet","a witchwood icon","an enchanted gem","bags of salt","broad arrows","earmuffs","Fishing explosives","fungicide spray","ice coolers","insulated boots","leaf-bladed spear","rock hammers","Slayer gloves","Slayer staff","super fishing explosive")