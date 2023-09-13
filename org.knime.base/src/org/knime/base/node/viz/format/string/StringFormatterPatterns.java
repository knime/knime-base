/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   30 Aug 2023 (jasper): created
 */
package org.knime.base.node.viz.format.string;

import java.util.regex.Pattern;

/**
 * Utility class that holds Regular Expression {@link Pattern}s that are used by the {@link StringFormatter}
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class StringFormatterPatterns {

    private StringFormatterPatterns() {
    }

    /**
     * Pattern that matches non-printable characters that shall be replaced by the {@link StringFormatter}
     */
    public static final Pattern NON_PRINTABLE_CHARS = //
        Pattern.compile("(?![\n\r ])[\\p{Cf}\\p{Cc}\\p{Z}]", Pattern.UNICODE_CHARACTER_CLASS);
    //                                             ^^^^^^ Separators
    //                                      ^^^^^^^ Control characters
    //                               ^^^^^^^ Formatting characters
    //                   ^^^^^^^^^^^ negative lookahead, don't match CR, LF and normal space

    /**
     * Pattern that matches email addresses in a string
     */
    public static final Pattern EMAIL;

    /**
     * Pattern that matches URLs in a string, but not the ones that are part of an email address
     */
    public static final Pattern URL;

    static {
        // Regular expression to match all IANA top-level domains. List accurate as of 2023-08-30.
        // List taken from: http://data.iana.org/TLD/tlds-alpha-by-domain.txt
        // This pattern is auto-generated with
        // android.googlesource.com/platform/frameworks/ex/+/refs/heads/main/common/tools/make-iana-tld-pattern.py
        final var iana_tlds = "(?i:"
            + "(?:aaa|aarp|abb|abbott|abbvie|abc|able|abogado|abudhabi|academy|accenture|accountant"
            + "|accountants|aco|actor|ads|adult|aeg|aero|aetna|afl|africa|agakhan|agency|aig|airbus"
            + "|airforce|airtel|akdn|alibaba|alipay|allfinanz|allstate|ally|alsace|alstom|amazon|americanexpress"
            + "|americanfamily|amex|amfam|amica|amsterdam|analytics|android|anquan|anz|aol|apartments"
            + "|app|apple|aquarelle|arab|aramco|archi|army|arpa|art|arte|asda|asia|associates|athleta"
            + "|attorney|auction|audi|audible|audio|auspost|author|auto|autos|avianca|aws|axa|azure"
            + "|a[cdefgilmoqrstuwxz])"
            + "|(?:baby|baidu|banamex|bananarepublic|band|bank|bar|barcelona|barclaycard|barclays"
            + "|barefoot|bargains|baseball|basketball|bauhaus|bayern|bbc|bbt|bbva|bcg|bcn|beats|beauty"
            + "|beer|bentley|berlin|best|bestbuy|bet|bharti|bible|bid|bike|bing|bingo|bio|biz|black"
            + "|blackfriday|blockbuster|blog|bloomberg|blue|bms|bmw|bnpparibas|boats|boehringer|bofa"
            + "|bom|bond|boo|book|booking|bosch|bostik|boston|bot|boutique|box|bradesco|bridgestone"
            + "|broadway|broker|brother|brussels|build|builders|business|buy|buzz|bzh|b[abdefghijmnorstvwyz])"
            + "|(?:cab|cafe|cal|call|calvinklein|cam|camera|camp|canon|capetown|capital|capitalone"
            + "|car|caravan|cards|care|career|careers|cars|casa|case|cash|casino|cat|catering|catholic"
            + "|cba|cbn|cbre|cbs|center|ceo|cern|cfa|cfd|chanel|channel|charity|chase|chat|cheap|chintai"
            + "|christmas|chrome|church|cipriani|circle|cisco|citadel|citi|citic|city|cityeats|claims"
            + "|cleaning|click|clinic|clinique|clothing|cloud|club|clubmed|coach|codes|coffee|college"
            + "|cologne|com|comcast|commbank|community|company|compare|computer|comsec|condos|construction"
            + "|consulting|contact|contractors|cooking|cool|coop|corsica|country|coupon|coupons|courses"
            + "|cpa|credit|creditcard|creditunion|cricket|crown|crs|cruise|cruises|cuisinella|cymru"
            + "|cyou|c[acdfghiklmnoruvwxyz])"
            + "|(?:dabur|dad|dance|data|date|dating|datsun|day|dclk|dds|deal|dealer|deals|degree"
            + "|delivery|dell|deloitte|delta|democrat|dental|dentist|desi|design|dev|dhl|diamonds|diet"
            + "|digital|direct|directory|discount|discover|dish|diy|dnp|docs|doctor|dog|domains|dot"
            + "|download|drive|dtv|dubai|dunlop|dupont|durban|dvag|dvr|d[ejkmoz])"
            + "|(?:earth|eat|eco|edeka|edu|education|email|emerck|energy|engineer|engineering|enterprises"
            + "|epson|equipment|ericsson|erni|esq|estate|etisalat|eurovision|eus|events|exchange|expert"
            + "|exposed|express|extraspace|e[cegrstu])"
            + "|(?:fage|fail|fairwinds|faith|family|fan|fans|farm|farmers|fashion|fast|fedex|feedback"
            + "|ferrari|ferrero|fidelity|fido|film|final|finance|financial|fire|firestone|firmdale"
            + "|fish|fishing|fit|fitness|flickr|flights|flir|florist|flowers|fly|foo|food|football"
            + "|ford|forex|forsale|forum|foundation|fox|free|fresenius|frl|frogans|frontdoor|frontier"
            + "|ftr|fujitsu|fun|fund|furniture|futbol|fyi|f[ijkmor])"
            + "|(?:gal|gallery|gallo|gallup|game|games|gap|garden|gay|gbiz|gdn|gea|gent|genting"
            + "|george|ggee|gift|gifts|gives|giving|glass|gle|global|globo|gmail|gmbh|gmo|gmx|godaddy"
            + "|gold|goldpoint|golf|goo|goodyear|goog|google|gop|got|gov|grainger|graphics|gratis|green"
            + "|gripe|grocery|group|guardian|gucci|guge|guide|guitars|guru|g[abdefghilmnpqrstuwy])"
            + "|(?:hair|hamburg|hangout|haus|hbo|hdfc|hdfcbank|health|healthcare|help|helsinki|here"
            + "|hermes|hiphop|hisamitsu|hitachi|hiv|hkt|hockey|holdings|holiday|homedepot|homegoods"
            + "|homes|homesense|honda|horse|hospital|host|hosting|hot|hotels|hotmail|house|how|hsbc"
            + "|hughes|hyatt|hyundai|h[kmnrtu])"
            + "|(?:ibm|icbc|ice|icu|ieee|ifm|ikano|imamat|imdb|immo|immobilien|inc|industries|infiniti"
            + "|info|ing|ink|institute|insurance|insure|int|international|intuit|investments|ipiranga"
            + "|irish|ismaili|ist|istanbul|itau|itv|i[delmnoqrst])"
            + "|(?:jaguar|java|jcb|jeep|jetzt|jewelry|jio|jll|jmp|jnj|jobs|joburg|jot|joy|jpmorgan"
            + "|jprs|juegos|juniper|j[emop])"
            + "|(?:kaufen|kddi|kerryhotels|kerrylogistics|kerryproperties|kfh|kia|kids|kim|kinder"
            + "|kindle|kitchen|kiwi|koeln|komatsu|kosher|kpmg|kpn|krd|kred|kuokgroup|kyoto|k[eghimnprwyz])"
            + "|(?:lacaixa|lamborghini|lamer|lancaster|land|landrover|lanxess|lasalle|lat|latino"
            + "|latrobe|law|lawyer|lds|lease|leclerc|lefrak|legal|lego|lexus|lgbt|lidl|life|lifeinsurance"
            + "|lifestyle|lighting|like|lilly|limited|limo|lincoln|link|lipsy|live|living|llc|llp|loan"
            + "|loans|locker|locus|lol|london|lotte|lotto|love|lpl|lplfinancial|ltd|ltda|lundbeck|luxe"
            + "|luxury|l[abcikrstuvy])"
            + "|(?:madrid|maif|maison|makeup|man|management|mango|map|market|marketing|markets|marriott"
            + "|marshalls|mattel|mba|mckinsey|med|media|meet|melbourne|meme|memorial|men|menu|merckmsd"
            + "|miami|microsoft|mil|mini|mint|mit|mitsubishi|mlb|mls|mma|mobi|mobile|moda|moe|moi|mom"
            + "|monash|money|monster|mormon|mortgage|moscow|moto|motorcycles|mov|movie|msd|mtn|mtr"
            + "|museum|music|m[acdeghklmnopqrstuvwxyz])"
            + "|(?:nab|nagoya|name|natura|navy|nba|nec|net|netbank|netflix|network|neustar|new|news"
            + "|next|nextdirect|nexus|nfl|ngo|nhk|nico|nike|nikon|ninja|nissan|nissay|nokia|norton"
            + "|now|nowruz|nowtv|nra|nrw|ntt|nyc|n[acefgilopruz])"
            + "|(?:obi|observer|office|okinawa|olayan|olayangroup|oldnavy|ollo|omega|one|ong|onl"
            + "|online|ooo|open|oracle|orange|org|organic|origins|osaka|otsuka|ott|ovh|om)"
            + "|(?:page|panasonic|paris|pars|partners|parts|party|pay|pccw|pet|pfizer|pharmacy|phd"
            + "|philips|phone|photo|photography|photos|physio|pics|pictet|pictures|pid|pin|ping|pink"
            + "|pioneer|pizza|place|play|playstation|plumbing|plus|pnc|pohl|poker|politie|porn|post"
            + "|pramerica|praxi|press|prime|pro|prod|productions|prof|progressive|promo|properties"
            + "|property|protection|pru|prudential|pub|pwc|p[aefghklmnrstwy])" + "|(?:qpon|quebec|quest|qa)"
            + "|(?:racing|radio|read|realestate|realtor|realty|recipes|red|redstone|redumbrella"
            + "|rehab|reise|reisen|reit|reliance|ren|rent|rentals|repair|report|republican|rest|restaurant"
            + "|review|reviews|rexroth|rich|richardli|ricoh|ril|rio|rip|rocher|rocks|rodeo|rogers|room"
            + "|rsvp|rugby|ruhr|run|rwe|ryukyu|r[eosuw])"
            + "|(?:saarland|safe|safety|sakura|sale|salon|samsclub|samsung|sandvik|sandvikcoromant"
            + "|sanofi|sap|sarl|sas|save|saxo|sbi|sbs|sca|scb|schaeffler|schmidt|scholarships|school"
            + "|schule|schwarz|science|scot|search|seat|secure|security|seek|select|sener|services"
            + "|seven|sew|sex|sexy|sfr|shangrila|sharp|shaw|shell|shia|shiksha|shoes|shop|shopping"
            + "|shouji|show|showtime|silk|sina|singles|site|ski|skin|sky|skype|sling|smart|smile|sncf"
            + "|soccer|social|softbank|software|sohu|solar|solutions|song|sony|soy|spa|space|sport"
            + "|spot|srl|stada|staples|star|statebank|statefarm|stc|stcgroup|stockholm|storage|store"
            + "|stream|studio|study|style|sucks|supplies|supply|support|surf|surgery|suzuki|swatch"
            + "|swiss|sydney|systems|s[abcdeghijklmnorstuvxyz])"
            + "|(?:tab|taipei|talk|taobao|target|tatamotors|tatar|tattoo|tax|taxi|tci|tdk|team|tech"
            + "|technology|tel|temasek|tennis|teva|thd|theater|theatre|tiaa|tickets|tienda|tips|tires"
            + "|tirol|tjmaxx|tjx|tkmaxx|tmall|today|tokyo|tools|top|toray|toshiba|total|tours|town"
            + "|toyota|toys|trade|trading|training|travel|travelers|travelersinsurance|trust|trv|tube"
            + "|tui|tunes|tushu|tvs|t[cdfghjklmnortvwz])" + "|(?:ubank|ubs|unicom|university|uno|uol|ups|u[agksyz])"
            + "|(?:vacations|vana|vanguard|vegas|ventures|verisign|versicherung|vet|viajes|video"
            + "|vig|viking|villas|vin|vip|virgin|visa|vision|viva|vivo|vlaanderen|vodka|volkswagen"
            + "|volvo|vote|voting|voto|voyage|v[aceginu])"
            + "|(?:wales|walmart|walter|wang|wanggou|watch|watches|weather|weatherchannel|webcam"
            + "|weber|website|wed|wedding|weibo|weir|whoswho|wien|wiki|williamhill|win|windows|wine"
            + "|winners|wme|wolterskluwer|woodside|work|works|world|wow|wtc|wtf|w[fs])"
            + "|(?:\u03b5\u03bb|\u03b5\u03c5|\u0431\u0433|\u0431\u0435\u043b|\u0434\u0435\u0442\u0438"
            + "|\u0435\u044e|\u043a\u0430\u0442\u043e\u043b\u0438\u043a|\u043a\u043e\u043c|\u043c\u043a\u0434"
            + "|\u043c\u043e\u043d|\u043c\u043e\u0441\u043a\u0432\u0430|\u043e\u043d\u043b\u0430\u0439\u043d"
            + "|\u043e\u0440\u0433|\u0440\u0443\u0441|\u0440\u0444|\u0441\u0430\u0439\u0442|\u0441\u0440\u0431"
            + "|\u0443\u043a\u0440|\u049b\u0430\u0437|\u0570\u0561\u0575|\u05d9\u05e9\u05e8\u05d0\u05dc"
            + "|\u05e7\u05d5\u05dd|\u0627\u0628\u0648\u0638\u0628\u064a|\u0627\u062a\u0635\u0627\u0644\u0627\u062a"
            + "|\u0627\u0631\u0627\u0645\u0643\u0648|\u0627\u0644\u0627\u0631\u062f\u0646"
            + "|\u0627\u0644\u0628\u062d\u0631\u064a\u0646"
            + "|\u0627\u0644\u062c\u0632\u0627\u0626\u0631|\u0627\u0644\u0633\u0639\u0648\u062f\u064a\u0629"
            + "|\u0627\u0644\u0639\u0644\u064a\u0627\u0646|\u0627\u0644\u0645\u063a\u0631\u0628"
            + "|\u0627\u0645\u0627\u0631\u0627\u062a"
            + "|\u0627\u06cc\u0631\u0627\u0646|\u0628\u0627\u0631\u062a|\u0628\u0627\u0632\u0627\u0631"
            + "|\u0628\u064a\u062a\u0643|\u0628\u06be\u0627\u0631\u062a|\u062a\u0648\u0646\u0633"
            + "|\u0633\u0648\u062f\u0627\u0646"
            + "|\u0633\u0648\u0631\u064a\u0629|\u0634\u0628\u0643\u0629|\u0639\u0631\u0627\u0642|\u0639\u0631\u0628"
            + "|\u0639\u0645\u0627\u0646|\u0641\u0644\u0633\u0637\u064a\u0646|\u0642\u0637\u0631"
            + "|\u0643\u0627\u062b\u0648\u0644\u064a\u0643"
            + "|\u0643\u0648\u0645|\u0645\u0635\u0631|\u0645\u0644\u064a\u0633\u064a\u0627"
            + "|\u0645\u0648\u0631\u064a\u062a\u0627\u0646\u064a\u0627"
            + "|\u0645\u0648\u0642\u0639|\u0647\u0645\u0631\u0627\u0647|\u067e\u0627\u06a9\u0633\u062a\u0627\u0646"
            + "|\u0680\u0627\u0631\u062a|\u0915\u0949\u092e|\u0928\u0947\u091f|\u092d\u093e\u0930\u0924"
            + "|\u092d\u093e\u0930\u0924\u092e\u094d|\u092d\u093e\u0930\u094b\u0924|\u0938\u0902\u0917\u0920\u0928"
            + "|\u09ac\u09be\u0982\u09b2\u09be|\u09ad\u09be\u09b0\u09a4|\u09ad\u09be\u09f0\u09a4"
            + "|\u0a2d\u0a3e\u0a30\u0a24"
            + "|\u0aad\u0abe\u0ab0\u0aa4|\u0b2d\u0b3e\u0b30\u0b24|\u0b87\u0ba8\u0bcd\u0ba4\u0bbf\u0baf\u0bbe"
            + "|\u0b87\u0bb2\u0b99\u0bcd\u0b95\u0bc8|\u0b9a\u0bbf\u0b99\u0bcd\u0b95\u0baa\u0bcd\u0baa\u0bc2\u0bb0\u0bcd"
            + "|\u0c2d\u0c3e\u0c30\u0c24\u0c4d|\u0cad\u0cbe\u0cb0\u0ca4|\u0d2d\u0d3e\u0d30\u0d24\u0d02"
            + "|\u0dbd\u0d82\u0d9a\u0dcf|\u0e04\u0e2d\u0e21|\u0e44\u0e17\u0e22|\u0ea5\u0eb2\u0ea7|\u10d2\u10d4"
            + "|\u307f\u3093\u306a|\u30a2\u30de\u30be\u30f3|\u30af\u30e9\u30a6\u30c9|\u30b0\u30fc\u30b0\u30eb"
            + "|\u30b3\u30e0|\u30b9\u30c8\u30a2|\u30bb\u30fc\u30eb|\u30d5\u30a1\u30c3\u30b7\u30e7\u30f3"
            + "|\u30dd\u30a4\u30f3\u30c8|\u4e16\u754c|\u4e2d\u4fe1|\u4e2d\u56fd|\u4e2d\u570b|\u4e2d\u6587\u7f51"
            + "|\u4e9a\u9a6c\u900a|\u4f01\u4e1a|\u4f5b\u5c71|\u4fe1\u606f|\u5065\u5eb7|\u516b\u5366"
            + "|\u516c\u53f8|\u516c\u76ca|\u53f0\u6e7e|\u53f0\u7063|\u5546\u57ce|\u5546\u5e97|\u5546\u6807"
            + "|\u5609\u91cc|\u5609\u91cc\u5927\u9152\u5e97|\u5728\u7ebf|\u5927\u62ff|\u5929\u4e3b\u6559"
            + "|\u5a31\u4e50|\u5bb6\u96fb|\u5e7f\u4e1c|\u5fae\u535a|\u6148\u5584|\u6211\u7231\u4f60"
            + "|\u624b\u673a|\u62db\u8058|\u653f\u52a1|\u653f\u5e9c|\u65b0\u52a0\u5761|\u65b0\u95fb"
            + "|\u65f6\u5c1a|\u66f8\u7c4d|\u673a\u6784|\u6de1\u9a6c\u9521|\u6e38\u620f|\u6fb3\u9580"
            + "|\u70b9\u770b|\u79fb\u52a8|\u7ec4\u7ec7\u673a\u6784|\u7f51\u5740|\u7f51\u5e97|\u7f51\u7ad9"
            + "|\u7f51\u7edc|\u8054\u901a|\u8c37\u6b4c|\u8d2d\u7269|\u901a\u8ca9|\u96c6\u56e2|\u96fb\u8a0a\u76c8\u79d1"
            + "|\u98de\u5229\u6d66|\u98df\u54c1|\u9910\u5385|\u9999\u683c\u91cc\u62c9|\u9999\u6e2f"
            + "|\ub2f7\ub137|\ub2f7\ucef4|\uc0bc\uc131|\ud55c\uad6d|verm\u00f6gensberater|verm\u00f6gensberatung"
            + "|xbox|xerox|xfinity|xihuan|xin|xn\\-\\-11b4c3d|xn\\-\\-1ck2e1b|xn\\-\\-1qqw23a|xn\\-\\-2scrj9c"
            + "|xn\\-\\-30rr7y|xn\\-\\-3bst00m|xn\\-\\-3ds443g|xn\\-\\-3e0b707e|xn\\-\\-3hcrj9c|xn\\-\\-3pxu8k"
            + "|xn\\-\\-42c2d9a|xn\\-\\-45br5cyl|xn\\-\\-45brj9c|xn\\-\\-45q11c|xn\\-\\-4dbrk0ce|xn\\-\\-4gbrim"
            + "|xn\\-\\-54b7fta0cc|xn\\-\\-55qw42g|xn\\-\\-55qx5d|xn\\-\\-5su34j936bgsg|xn\\-\\-5tzm5g"
            + "|xn\\-\\-6frz82g|xn\\-\\-6qq986b3xl|xn\\-\\-80adxhks|xn\\-\\-80ao21a|xn\\-\\-80aqecdr1a"
            + "|xn\\-\\-80asehdb|xn\\-\\-80aswg|xn\\-\\-8y0a063a|xn\\-\\-90a3ac|xn\\-\\-90ae|xn\\-\\-90ais"
            + "|xn\\-\\-9dbq2a|xn\\-\\-9et52u|xn\\-\\-9krt00a|xn\\-\\-b4w605ferd|xn\\-\\-bck1b9a5dre4c"
            + "|xn\\-\\-c1avg|xn\\-\\-c2br7g|xn\\-\\-cck2b3b|xn\\-\\-cckwcxetd|xn\\-\\-cg4bki"
            + "|xn\\-\\-clchc0ea0b2g2a9gcd"
            + "|xn\\-\\-czr694b|xn\\-\\-czrs0t|xn\\-\\-czru2d|xn\\-\\-d1acj3b|xn\\-\\-d1alf|xn\\-\\-e1a4c"
            + "|xn\\-\\-eckvdtc9d|xn\\-\\-efvy88h|xn\\-\\-fct429k|xn\\-\\-fhbei|xn\\-\\-fiq228c5hs"
            + "|xn\\-\\-fiq64b|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fjq720a|xn\\-\\-flw351e|xn\\-\\-fpcrj9c3d"
            + "|xn\\-\\-fzc2c9e2c|xn\\-\\-fzys8d69uvgm|xn\\-\\-g2xx48c|xn\\-\\-gckr3f0f|xn\\-\\-gecrj9c"
            + "|xn\\-\\-gk3at1e|xn\\-\\-h2breg3eve|xn\\-\\-h2brj9c|xn\\-\\-h2brj9c8c|xn\\-\\-hxt814e"
            + "|xn\\-\\-i1b6b1a6a2e|xn\\-\\-imr513n|xn\\-\\-io0a7i|xn\\-\\-j1aef|xn\\-\\-j1amh|xn\\-\\-j6w193g"
            + "|xn\\-\\-jlq480n2rg|xn\\-\\-jvr189m|xn\\-\\-kcrx77d1x4a|xn\\-\\-kprw13d|xn\\-\\-kpry57d"
            + "|xn\\-\\-kput3i|xn\\-\\-l1acc|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgb9awbf|xn\\-\\-mgba3a3ejt"
            + "|xn\\-\\-mgba3a4f16a|xn\\-\\-mgba7c0bbn0a|xn\\-\\-mgbaakc7dvf|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbab2bd"
            + "|xn\\-\\-mgbah1a3hjkrd|xn\\-\\-mgbai9azgqp6j|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a|xn\\-\\-mgbbh1a71e"
            + "|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgbca7dzdo|xn\\-\\-mgbcpq6gpa1a|xn\\-\\-mgberp4a5d4ar|xn\\-\\-mgbgu82a"
            + "|xn\\-\\-mgbi4ecexp|xn\\-\\-mgbpl2fh|xn\\-\\-mgbt3dhd|xn\\-\\-mgbtx2b|xn\\-\\-mgbx4cd0ab"
            + "|xn\\-\\-mix891f|xn\\-\\-mk1bu44c|xn\\-\\-mxtq1m|xn\\-\\-ngbc5azd|xn\\-\\-ngbe9e0a|xn\\-\\-ngbrx"
            + "|xn\\-\\-node|xn\\-\\-nqv7f|xn\\-\\-nqv7fs00ema|xn\\-\\-nyqy26a|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl"
            + "|xn\\-\\-otu796d|xn\\-\\-p1acf|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-pssy2u|xn\\-\\-q7ce6a"
            + "|xn\\-\\-q9jyb4c|xn\\-\\-qcka1pmc|xn\\-\\-qxa6a|xn\\-\\-qxam|xn\\-\\-rhqv96g|xn\\-\\-rovu88b"
            + "|xn\\-\\-rvc1e0am3e|xn\\-\\-s9brj9c|xn\\-\\-ses554g|xn\\-\\-t60b56a|xn\\-\\-tckwe|xn\\-\\-tiq49xqyj"
            + "|xn\\-\\-unup4y|xn\\-\\-vermgensberater\\-ctb|xn\\-\\-vermgensberatung\\-pwb|xn\\-\\-vhquv"
            + "|xn\\-\\-vuq861b|xn\\-\\-w4r85el8fhu5dnra|xn\\-\\-w4rs40l|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a"
            + "|xn\\-\\-xhq521b|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-y9a3aq|xn\\-\\-yfro4i67o"
            + "|xn\\-\\-ygbi2ammx|xn\\-\\-zfr164b|xxx|xyz)"
            + "|(?:yachts|yahoo|yamaxun|yandex|yodobashi|yoga|yokohama|you|youtube|yun|y[et])"
            + "|(?:zappos|zara|zero|zip|zone|zuerich|z[amw]))";
        // a simpler pattern to match a top-level domain: support TLDs like .de or .org but also Punycode (See RFC-3492)
        // unfortunately, this also matches e.g. `lastname` as a valid TLD, so that any `firstname.lastname` is matched
        @SuppressWarnings("unused") // Kept here for reference
        final var simple_tld = "(?:xn\\-\\-[\\w\\-]{0,58}\\w|\\p{Alpha}{2,63})";
        // characters that are allowed in an email before the '@' sign
        final var email_chars = "\\p{Alnum}\\+\\-_%'";
        // the email part before the '@' sign: up to 64 characters, with dots allowed everywhere but the start and end
        final var email_local = "[" + email_chars + "](?:[" + email_chars + "\\.]{0,62}[" + email_chars + "])?";
        // domain names -- up to 63 characters per subdomain; _ and - allowed everywhere but the start and end
        final var domain = "(?:(?:\\p{Alnum}|\\p{Alnum}[\\p{Alnum}_\\-]{0,61}\\p{Alnum})\\.)+" + iana_tlds;
        // a protocol that could start a URL (case-insensitive)
        final var protocol = "(?i:https?)://";
        // any number between 0 to 255, with no trailing zeroes
        final var ipv4_num = "(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])";
        // any one-to-four-digit hex number, possibly with trailing zeroes
        final var ipv6_num = "(?i:[0-9a-f]{1,4})";
        // an IP address
        final var ip = "(?:" + ipv4_num + "\\.){3}" + ipv4_num + "|(?:" + ipv6_num + ":){7}" + ipv6_num;
        // single characters that are allowed in a query
        final var query_chars = "\\p{Alnum};/\\?:@&=#~\\-\\.\\+!\\*'\\(\\),_\\$";
        // the path and query string at the end of an URL, e.g. "/v1/user" or "/index.html?user=knime" or similar
        final var query = "[/\\?](?:[" + query_chars + "]|(?i:%[a-f0-9]{2}))*";
        // an URL, including optional protocol, optional username, domain or ip, optional port, and query
        final var url =
            "(?:" + protocol + "(?:\\p{Alnum}+@)?)?(?:" + domain + "|" + ip + ")(?:\\:\\d{1,5})?(?:" + query + ")?";

        // start and end lookarounds
        final var email_start = "(?<=^|[^" + email_chars + "\\.])(?<!=\")";
        final var email_end = "(?=$|\\W)";
        final var url_start = "(?<=^|[^\\p{Alnum}_\\-\\.@/])(?<!=\")";
        final var url_end = "(?=$|[^" + query_chars + "]|\\b)";

        // pattern that matches email addresses
        final var email_regex = "(" + email_local + "@" + domain + ")";
        // pattern that matches URLs
        final var url_regex = "(" + url + ")";

        // compile the patterns, adding checks that the URL is not cut off by an ellipsis. This is only an approximation
        // since the ellipsis could also be just before the url/email and not (figuratively) inside the url/email, but
        // rather make one hyperlink too few than to link to e.g. oogle.com or some other site that is not intended to
        // be linked to.
        // In both cases, the first capture group is the address and can be retrieved with "$1" in a replace function
        EMAIL = Pattern.compile(//
            email_start + "(?<!\\Q" + StringFormatter.ELLIPSIS + "\\E)" + email_regex + "(?!\\Q"
                + StringFormatter.ELLIPSIS + "\\E)" + email_end,
            Pattern.UNICODE_CHARACTER_CLASS);
        URL = Pattern.compile(//
            url_start + "(?<!\\Q" + StringFormatter.ELLIPSIS + "\\E)" + url_regex + "(?!\\Q" + StringFormatter.ELLIPSIS
                + "\\E)" + url_end,
            Pattern.UNICODE_CHARACTER_CLASS);
    }

}
