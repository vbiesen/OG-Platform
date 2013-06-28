/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.maths.lowlevelapi.slatec.fnlib;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

import com.opengamma.maths.lowlevelapi.functions.FPEquals;

/**
 * tests dgamma
 */
@Test
public class DLNGAMTest {

  private double[] input = new double[] {-21.5e0, -19.5e0, -17.5e0, -15.5e0, -13.5e0, -11.5e0, -9.5e0, -7.5e0, -5.5e0, -3.5e0, -1.5e0, 0.5e0, 1e0, 1.5e0, 2e0, 2.5e0, 3e0, 3.5e0, 4e0, 4.5e0, 5e0,
      5.5e0, 6e0, 6.5e0, 7e0, 7.5e0, 8e0, 8.5e0, 9e0, 9.5e0, 10e0, 10.5e0, 11e0, 11.5e0, 12e0, 12.5e0, 13e0, 13.5e0, 14e0, 14.5e0, 15e0, 15.5e0, 16e0, 16.5e0, 17e0, 17.5e0, 18e0, 18.5e0, 19e0,
      19.5e0, 20e0 };

  private double[] expected = new double[] {//
  -4.5775248909959377544137801752880363183831504447904782510119890570824093732034518740209073311482459573057494988288448215105e1, //
      -3.9686771088681397935632660109413461696046958062344435969550302414628043185695264226920890826019312850206001780566118303187e1, //
      -3.3798585891027417682650296002810523358845122241314353745993009908120881722101273446460405528085759986950967569157643419899e1, //
      -2.8133024629191414386321459354169854121364239014424318716909069842927842198449090165305669342581179344039256779017122223280e1, //
      -2.2718035955839684732043487240181938441570787438741493174472041437796867244653076031010164050420073665904283219444056915212e1, //
      -1.7589617626087045527490697105878339184728113325456719883684022672922813633521323032340775683785355553024443488243893563644e1, //
      -1.2795895333554363459017810536618790768152157991932184009732662830851533475505235930692826992223594497973217513441895791565e1, //
      -8.4045373714515975375937127297741638314775759867678209121388248755467354415794967922783003943631890297418315802150999693881, //
      -4.5178321740077413543786849609764850186502163986368571303137971587583847372682093125867027520086252164686400180758051905803, //
      -1.3090066849930420463607151520826574456845286977744515598672008013396097708571440969761431883713636225619871960968928320397, //
      8.6004701537648101451093268167035678732715711735541684226347722108536183566319350422992253431685014014703783908953250030429e-1, //
      5.7236494292470008707171367567652935582364740645765578575681153573606888494241303989181163513774485385100490611434899457939e-1, //
      0, //
      -1.2078223763524522234551844578164721225185272790259946836386847375732473702728167571405169185867383369099657490622169115418e-1, //
      0, //
      2.8468287047291915963249466968270192432013769555989472925014585038677593422163257555370073595863956755497197313916548885450e-1, //
      6.9314718055994530941723212145817656807550013436025525412068000949339362196969471560586332699641868754200148102057068573342e-1, //
      1.2009736023470742248160218814507129957702389154681571970421137323675612999595956245779712870554004908129367951403103660200, //
      1.7917594692280550008124773583807022727229906921830047058553743431308879151883036824794790818101507763299715100865285514757, //
      2.4537365708424422205041425034357161573318235106897631313808238728117475407419701767597664586468533302639970083255908211983, //
      3.1780538303479456196469416012970554088739909609035152140967343621176751591276931136912057358029881514139744721276699229433, //
      3.9578139676187162938774008558225909985513044919750067807295325305933425052094933949011346412778988202979355854369358669488, //
      4.7874917427820459942477009345232430483995923151720329360093822535918541468353508783213396138961677622139407751493854858423, //
      5.6625620598571415285221123123295437302975112115521687018274202302305225071530353923703260222841149241706498303045031797351, //
      6.5792512120101009950601782929039453211225830073550376418647565967227420620236545608008186957063185385439122852359140373173, //
      7.5343642367587329551583676324366857670272790219521205641257857314005935525077508561640373587281857253979065413766952156343, //
      8.5251613610654143001655310363471250507596677369368988303241467466603219247757238285884771942941900655369739794417651782275, //
      9.5492672573009977117371400811272225431248707996831324836524479470188732114643228720619236646386787374438413924437979585430, //
      1.0604602902745250228417227400721654754986168140017664592686186775140502790684807975406067175283446128162978422503477235430e1, //
      1.1689333420797268482569442577542172510637573677908622016829005675407772165295559437368155205679762997420188173470414330101e1, //
      1.2801827480081469611207717874566706164281149255663163496155575442415491377122025909153298684910910305738918480635392966911e1, //
      1.3940625219403763633161237887971849479799452804847495581246285902323671245390062010476450262499084205675227325670593780713e1, //
      1.5104412573075515295225709329251070371882250744291936472188903343383063986799378389389295890000508604080886264677679215548e1, //
      1.6292000476567241320244603746879378346008527957891850967319690376405351779391045529531861188904269133914257567921832101629e1, //
      1.7502307845873885839287652907216199671703957598229353647407471052513637610712615102464350598003143395495601990565817214070e1, //
      1.8734347511936445701634124457231397896375408138372031455197645744394951403406149112124398954060845260726453300472591552798e1, //
      1.9987214495661886149517362387055078512502448424772613607383525405137919147870613500549693006809712859367574981672916451269e1, //
      2.1260076156244701141418411002225596607351110712548811644902261517849915756851769925778803383250785794784384425495451992862e1, //
      2.2552163853123422885570849828620397117307716369532820723802570915801383815195023679949267670250202348136833173765679172906e1, //
      2.3862765841689084906186914591534997153218082251656804745985664509269005014537902110793787320695563373606293031672754904353e1, //
      2.5191221182738681500093434693521753415020301233474937166382641075232357299916787663342789495834492562671896348992100999547e1, //
      2.6536914491115613623952954502438732190637095031219293570786654851418392173706059728481291108517001108710291135377351053345e1, //
      2.7899271383840891566089439263670466759193393145566204340029983300344030580843054394846539128741404262259832681079774428193e1, //
      2.9277754515040814560464886705522912833011533827339630288422692914399979968333916245089292612856669051741266591245820212422e1, //
      3.0671860106080672803758367749503173031495393683007225356512703338317605068721833257269992436727079012427838605162057171130e1, //
      3.2081114895947349486504843398952391269405231104739541661255274947674654263496067209432099748676617244401950865179345390953e1, //
      3.3505073450136888884007902367376299567083596695592970143809941076199897644522764538182087304764581959946186867209244228404e1, //
      3.4943315776876817856793723354163582070492417054229665317506632979593019491986099526244028798361249694652977381386341409028e1, //
      3.6395445208033053576215624962679527544454077945598724301400009752968279852929677187535182141388464825064128406361730645633e1, //
      3.7861086508961096991744586903736852666316994507037046227030865651292615617006765876037187005036999667892784852656666390689e1, //
      3.9339884187199494036224652394567381081691457206897853119937969989377572554993874476249340525204204720861169039582480781990e1, //
      4.0831500974530798109776087460766520407694252875259747541063925486100180955580090306704514096294802557908011592794816292316e1 };

  @Test
  public void dgamlnSmokeTest() {
    double ans;
    for (int i = 0; i < input.length; i++) {
      ans = DLNGAM.dlngam(input[i]);
      assertTrue(FPEquals.fuzzyEquals(expected[i], ans));
    }
  }

}