package com.watchyourself.watchperiod.domain

/** Short, user-facing source list for the in-app information screens. */
object Sources {
    val disclaimer =
        "Die Inhalte dienen der Information und ersetzen keine ärztliche Beratung. " +
            "Die Hormonkurven und die Auswertungen sind schematische Modelle, keine Messung."

    val items = listOf(
        "Wei SM et al. Differential Effects of Ovarian Steroids in Women With and Without Premenstrual Dysphoric Disorder. Am J Psychiatry 2025;182(10):922–934. doi:10.1176/appi.ajp.20240596",
        "Bixo M, Stiernman L, Bäckström T. Neurosteroids and premenstrual dysphoric disorder. Br J Psychiatry 2026;229(1):92–100. doi:10.1192/bjp.2025.103",
        "Rapkin AJ, Akopians AL. Pathophysiology of premenstrual syndrome and premenstrual dysphoric disorder. Menopause Int 2012;18(2):52–59. doi:10.1258/mi.2012.012014",
        "Biggs WS, Demuth RH. Premenstrual syndrome and premenstrual dysphoric disorder. Am Fam Physician 2011;84(8):918–924.",
        "Freeman EW. Premenstrual syndrome and premenstrual dysphoric disorder: definitions and diagnosis. Psychoneuroendocrinology 2003;28 Suppl 3:25–37. doi:10.1016/s0306-4530(03)00099-4",
        "Schmidt PJ et al. Circulating levels of anxiolytic steroids in the luteal phase in women with premenstrual syndrome and in control subjects. J Clin Endocrinol Metab 1994;79(5):1256–1260. doi:10.1210/jcem.79.5.7962316",
        "Martinez PE et al. 5α-Reductase Inhibition Prevents the Luteal Phase Increase in Plasma Allopregnanolone Levels and Mitigates Symptoms in Women with PMDD. Neuropsychopharmacology 2016;41(4):1093–1102. doi:10.1038/npp.2015.246",
        "Bull JR et al. Real-world menstrual cycle characteristics of more than 600,000 menstrual cycles. NPJ Digit Med 2019;2:83. doi:10.1038/s41746-019-0152-7",
        "Barron ML, Fehring RJ. Basal body temperature assessment: is it useful to couples seeking pregnancy? MCN Am J Matern Child Nurs 2005;30(5):290–296. doi:10.1097/00005721-200509000-00004",
        "Baker FC, Siboza F, Fuller A. Temperature regulation in women: effects of the menstrual cycle. Temperature (Austin) 2020;7(3):226–262. doi:10.1080/23328940.2020.1735927",
        "Nolte J et al. Optimizing basal body temperature measurement for cycle diagnostics: a comparison of different methods in female recreational athletes. Front Sports Act Living 2026;7:1732233. doi:10.3389/fspor.2025.1732233",
        "Huang G et al. Circulating dihydrotestosterone, testosterone, and free testosterone levels in healthy women across the menstrual cycle. Fertil Steril 2022;118(6):1150–1158. doi:10.1016/j.fertnstert.2022.09.011",
        "Stern J, Casto K. Salivary testosterone across the menstrual cycle. Horm Behav 2024;164:105608. doi:10.1016/j.yhbeh.2024.105608",
        "Handy AB et al. Psychiatric Symptoms Across the Menstrual Cycle in Adult Women: A Comprehensive Review. Harv Rev Psychiatry 2022;30(2):100–117. doi:10.1097/HRP.0000000000000329",
        "Sundström Poromaa I, Gingnell M. Menstrual cycle influence on cognitive function and emotion processing. Front Neurosci 2014;8:380. doi:10.3389/fnins.2014.00380",
        "Rubinow DR, Schmidt PJ, Roca CA. Estrogen-serotonin interactions: implications for affective regulation. Biol Psychiatry 1998;44(9):839–850. doi:10.1016/s0006-3223(98)00162-0",
        "van Stein KR et al. Ovulatory Shifts in Sexual Desire But Not Mate Preferences. Evol Psychol 2019;17(2):1474704919848116. doi:10.1177/1474704919848116",
        "Pletzer B et al. Menstrual cycle variations in stress vulnerability and sociability relate to mental health symptoms and libido. NPJ Womens Health 2026;4:18. doi:10.1038/s44294-026-00140-z",
        "Yonkers KA, Simoni MK. Premenstrual disorders. Am J Obstet Gynecol 2018;218(1):68–74. doi:10.1016/j.ajog.2017.05.045",
        "Hantsoo L, Epperson CN. Premenstrual Dysphoric Disorder: Epidemiology and Treatment. Curr Psychiatry Rep 2015;17(11):87. doi:10.1007/s11920-015-0628-3",
        "Roney JR, Simmons ZL. Sexual interest and activity across the menstrual cycle: systematic review and meta-analysis. Hum Reprod Update 2018;24(1):19–35. doi:10.1093/humupd/dmx033",
        "Fehlner K, Zimmermann V et al. The Premenstrual Syndrome and the Partner Relationship: How it affects both partners in different ways. Tagebuchstudie über einen Zyklus mit beiden Partnern, 2018.",
        "Frank B, Dixon D, Grosz H. Conjoint monitoring of symptoms of premenstrual syndrome: impact on marital satisfaction. J Couns Psychol 1993;40(1):109–114.",
        "Mishra S et al. Premenstrual Disorders. StatPearls, NCBI Bookshelf 2023.",
        "Beltran L et al. Serum total prolactin and monomeric prolactin reference intervals determined by precipitation with polyethylene glycol. Clin Chem 2008;54(10):1673–1681. doi:10.1373/clinchem.2008.105312",
        "Tachibana A et al. Changes in Serum Oxytocin Levels under Physiological and Supraphysiological Gonadal Steroid Hormone Conditions in Women of Reproductive Age. Nutrients 2022;14(24):5350. doi:10.3390/nu14245350",
        "Makanji Y et al. Inhibin at 90: from discovery to clinical application, a historical review. Endocr Rev 2014;35(5):747–794. doi:10.1210/er.2014-1003",
        "Marshall SA et al. The Role of Relaxin in Normal and Abnormal Uterine Function During the Menstrual Cycle and Early Pregnancy. Reprod Sci 2017;24(3):342–354. doi:10.1177/1933719116657189",
        "Skorupskaite K, George JT, Anderson RA. The kisspeptin-GnRH pathway in human reproductive health and disease. Hum Reprod Update 2014;20(4):485–500. doi:10.1093/humupd/dmu009",
        "Thurston L et al. Effects of Kisspeptin Administration in Women With Hypoactive Sexual Desire Disorder: A Randomized Clinical Trial. JAMA Netw Open 2022;5(10):e2236131. doi:10.1001/jamanetworkopen.2022.36131",
        "Kudielka BM, Kirschbaum C. Sex differences in HPA axis responses to stress: a review. Biol Psychol 2005;69(1):113–132. doi:10.1016/j.biopsycho.2004.11.009",
        "Ferries-Rowe E, Corey E, Archer JS. Primary Dysmenorrhea: Diagnosis and Therapy. Obstet Gynecol 2020;136(5):1047–1058. doi:10.1097/AOG.0000000000004096",
    )
}
