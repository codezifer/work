package de.carsten.android.muzzic.id3.model.visitor

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.frame.LanguageDescriptorFrame
import de.carsten.android.muzzic.id3.model.frame.OwnerIdentifierFrame
import de.carsten.android.muzzic.id3.model.frame.TextInformationFrame
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.frame.UrlLinkFrame
import de.carsten.android.muzzic.id3.model.frame.binary.AencFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.binary.AspiFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ComrFrame
import de.carsten.android.muzzic.id3.model.frame.binary.EncrFrame
import de.carsten.android.muzzic.id3.model.frame.binary.Equ2Frame
import de.carsten.android.muzzic.id3.model.frame.binary.EquaFrame
import de.carsten.android.muzzic.id3.model.frame.binary.EtcoFrame
import de.carsten.android.muzzic.id3.model.frame.binary.GeobFrame
import de.carsten.android.muzzic.id3.model.frame.binary.GridFrame
import de.carsten.android.muzzic.id3.model.frame.binary.IplsFrame
import de.carsten.android.muzzic.id3.model.frame.binary.LinkFrame
import de.carsten.android.muzzic.id3.model.frame.binary.McdiFrame
import de.carsten.android.muzzic.id3.model.frame.binary.MlltFrame
import de.carsten.android.muzzic.id3.model.frame.binary.OwneFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PcntFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PopmFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PossFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PrivFrame
import de.carsten.android.muzzic.id3.model.frame.binary.RbufFrame
import de.carsten.android.muzzic.id3.model.frame.binary.Rva2Frame
import de.carsten.android.muzzic.id3.model.frame.binary.RvadFrame
import de.carsten.android.muzzic.id3.model.frame.binary.RvrbFrame
import de.carsten.android.muzzic.id3.model.frame.binary.SeekFrame
import de.carsten.android.muzzic.id3.model.frame.binary.SignFrame
import de.carsten.android.muzzic.id3.model.frame.binary.SytcFrame
import de.carsten.android.muzzic.id3.model.frame.binary.UfidFrame
import de.carsten.android.muzzic.id3.model.frame.language.CommFrame
import de.carsten.android.muzzic.id3.model.frame.language.SyltFrame
import de.carsten.android.muzzic.id3.model.frame.language.UserFrame
import de.carsten.android.muzzic.id3.model.frame.language.UsltFrame
import de.carsten.android.muzzic.id3.model.frame.text.TalbFrame
import de.carsten.android.muzzic.id3.model.frame.text.TbpmFrame
import de.carsten.android.muzzic.id3.model.frame.text.TcomFrame
import de.carsten.android.muzzic.id3.model.frame.text.TconFrame
import de.carsten.android.muzzic.id3.model.frame.text.TcopFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdatFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdenFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdlyFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdorFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdrcFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdrlFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdtgFrame
import de.carsten.android.muzzic.id3.model.frame.text.TencFrame
import de.carsten.android.muzzic.id3.model.frame.text.TextFrame
import de.carsten.android.muzzic.id3.model.frame.text.TfltFrame
import de.carsten.android.muzzic.id3.model.frame.text.TimeFrame
import de.carsten.android.muzzic.id3.model.frame.text.TiplFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tit1Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tit3Frame
import de.carsten.android.muzzic.id3.model.frame.text.TkeyFrame
import de.carsten.android.muzzic.id3.model.frame.text.TlanFrame
import de.carsten.android.muzzic.id3.model.frame.text.TlenFrame
import de.carsten.android.muzzic.id3.model.frame.text.TmclFrame
import de.carsten.android.muzzic.id3.model.frame.text.TmedFrame
import de.carsten.android.muzzic.id3.model.frame.text.TmooFrame
import de.carsten.android.muzzic.id3.model.frame.text.ToalFrame
import de.carsten.android.muzzic.id3.model.frame.text.TofnFrame
import de.carsten.android.muzzic.id3.model.frame.text.TolyFrame
import de.carsten.android.muzzic.id3.model.frame.text.TopeFrame
import de.carsten.android.muzzic.id3.model.frame.text.ToryFrame
import de.carsten.android.muzzic.id3.model.frame.text.TownFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe1Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe2Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe3Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe4Frame
import de.carsten.android.muzzic.id3.model.frame.text.TposFrame
import de.carsten.android.muzzic.id3.model.frame.text.TproFrame
import de.carsten.android.muzzic.id3.model.frame.text.TpubFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrckFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrdaFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrsnFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrsoFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsizFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsoaFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsopFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsotFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsrcFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsseFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsstFrame
import de.carsten.android.muzzic.id3.model.frame.text.TxxxFrame
import de.carsten.android.muzzic.id3.model.frame.text.TyerFrame
import de.carsten.android.muzzic.id3.model.frame.url.WcomFrame
import de.carsten.android.muzzic.id3.model.frame.url.WcopFrame
import de.carsten.android.muzzic.id3.model.frame.url.WoafFrame
import de.carsten.android.muzzic.id3.model.frame.url.WoarFrame
import de.carsten.android.muzzic.id3.model.frame.url.WoasFrame
import de.carsten.android.muzzic.id3.model.frame.url.WorsFrame
import de.carsten.android.muzzic.id3.model.frame.url.WpayFrame
import de.carsten.android.muzzic.id3.model.frame.url.WpubFrame
import de.carsten.android.muzzic.id3.model.frame.url.WxxxFrame

/**
 * Hierarchical visitor over all ID3v2 frames.
 *
 * Specific `visitX` methods default to their group method (`visitText`,
 * `visitUrl`, `visitLanguage`, `visitOwner`), which in turn default to
 * [visitGeneric]. Implementations therefore only override the levels
 * they care about: a metadata mapper overrides [visitText] plus a few
 * specials, a validator may only override [visitGeneric].
 *
 * @param R result type of the visit operation.
 */
interface Id3FrameVisitor<R> {
    /**
     * Generic fallback for every frame.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitGeneric(frame: Id3Frame): R

    /**
     * Fallback for unknown or experimental frames.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitUnknown(frame: UnknownFrame): R = visitGeneric(frame)

    /**
     * Group handler for all text information frames.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitText(frame: TextInformationFrame): R = visitGeneric(frame)

    /**
     * Group handler for all URL link frames.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitUrl(frame: UrlLinkFrame): R = visitGeneric(frame)

    /**
     * Group handler for language-bearing descriptor frames.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitLanguage(frame: LanguageDescriptorFrame): R = visitGeneric(frame)

    /**
     * Group handler for owner-identifier frames.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitOwner(frame: OwnerIdentifierFrame): R = visitGeneric(frame)

    /**
     * Group handler for binary container frames without shared fields.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitBinary(frame: BinaryDataFrame): R = visitGeneric(frame)

    // Text information frames: default to visitText.
    fun visitTit1(frame: Tit1Frame): R = visitText(frame)
    fun visitTit2(frame: Tit2Frame): R = visitText(frame)
    fun visitTit3(frame: Tit3Frame): R = visitText(frame)
    fun visitTalb(frame: TalbFrame): R = visitText(frame)
    fun visitToal(frame: ToalFrame): R = visitText(frame)
    fun visitTrck(frame: TrckFrame): R = visitText(frame)
    fun visitTpos(frame: TposFrame): R = visitText(frame)
    fun visitTsst(frame: TsstFrame): R = visitText(frame)
    fun visitTsrc(frame: TsrcFrame): R = visitText(frame)
    fun visitTpe1(frame: Tpe1Frame): R = visitText(frame)
    fun visitTpe2(frame: Tpe2Frame): R = visitText(frame)
    fun visitTpe3(frame: Tpe3Frame): R = visitText(frame)
    fun visitTpe4(frame: Tpe4Frame): R = visitText(frame)
    fun visitTope(frame: TopeFrame): R = visitText(frame)
    fun visitTextFrame(frame: TextFrame): R = visitText(frame)
    fun visitToly(frame: TolyFrame): R = visitText(frame)
    fun visitTcom(frame: TcomFrame): R = visitText(frame)
    fun visitTmcl(frame: TmclFrame): R = visitText(frame)
    fun visitTipl(frame: TiplFrame): R = visitText(frame)
    fun visitTenc(frame: TencFrame): R = visitText(frame)
    fun visitTbpm(frame: TbpmFrame): R = visitText(frame)
    fun visitTlen(frame: TlenFrame): R = visitText(frame)
    fun visitTkey(frame: TkeyFrame): R = visitText(frame)
    fun visitTlan(frame: TlanFrame): R = visitText(frame)
    fun visitTcon(frame: TconFrame): R = visitText(frame)
    fun visitTflt(frame: TfltFrame): R = visitText(frame)
    fun visitTmed(frame: TmedFrame): R = visitText(frame)
    fun visitTmoo(frame: TmooFrame): R = visitText(frame)
    fun visitTcop(frame: TcopFrame): R = visitText(frame)
    fun visitTpro(frame: TproFrame): R = visitText(frame)
    fun visitTpub(frame: TpubFrame): R = visitText(frame)
    fun visitTown(frame: TownFrame): R = visitText(frame)
    fun visitTrsn(frame: TrsnFrame): R = visitText(frame)
    fun visitTrso(frame: TrsoFrame): R = visitText(frame)
    fun visitTofn(frame: TofnFrame): R = visitText(frame)
    fun visitTdly(frame: TdlyFrame): R = visitText(frame)
    fun visitTden(frame: TdenFrame): R = visitText(frame)
    fun visitTdor(frame: TdorFrame): R = visitText(frame)
    fun visitTdrc(frame: TdrcFrame): R = visitText(frame)
    fun visitTdrl(frame: TdrlFrame): R = visitText(frame)
    fun visitTdtg(frame: TdtgFrame): R = visitText(frame)
    fun visitTsse(frame: TsseFrame): R = visitText(frame)
    fun visitTsoa(frame: TsoaFrame): R = visitText(frame)
    fun visitTsop(frame: TsopFrame): R = visitText(frame)
    fun visitTsot(frame: TsotFrame): R = visitText(frame)
    fun visitTdat(frame: TdatFrame): R = visitText(frame)
    fun visitTimeFrame(frame: TimeFrame): R = visitText(frame)
    fun visitTory(frame: ToryFrame): R = visitText(frame)
    fun visitTrda(frame: TrdaFrame): R = visitText(frame)
    fun visitTsiz(frame: TsizFrame): R = visitText(frame)
    fun visitTyer(frame: TyerFrame): R = visitText(frame)

    // User defined text frame: standalone.
    fun visitTxxx(frame: TxxxFrame): R = visitGeneric(frame)

    // URL link frames: default to visitUrl.
    fun visitWcom(frame: WcomFrame): R = visitUrl(frame)
    fun visitWcop(frame: WcopFrame): R = visitUrl(frame)
    fun visitWoaf(frame: WoafFrame): R = visitUrl(frame)
    fun visitWoar(frame: WoarFrame): R = visitUrl(frame)
    fun visitWoas(frame: WoasFrame): R = visitUrl(frame)
    fun visitWors(frame: WorsFrame): R = visitUrl(frame)
    fun visitWpay(frame: WpayFrame): R = visitUrl(frame)
    fun visitWpub(frame: WpubFrame): R = visitUrl(frame)

    // User defined URL frame: standalone.
    fun visitWxxx(frame: WxxxFrame): R = visitGeneric(frame)

    // Language descriptor frames: default to visitLanguage.
    fun visitComm(frame: CommFrame): R = visitLanguage(frame)
    fun visitUslt(frame: UsltFrame): R = visitLanguage(frame)
    fun visitSylt(frame: SyltFrame): R = visitLanguage(frame)

    // Terms of use: standalone.
    fun visitUser(frame: UserFrame): R = visitGeneric(frame)

    // Owner identifier frames: default to visitOwner.
    fun visitUfid(frame: UfidFrame): R = visitOwner(frame)
    fun visitAenc(frame: AencFrame): R = visitOwner(frame)
    fun visitEncr(frame: EncrFrame): R = visitOwner(frame)
    fun visitGrid(frame: GridFrame): R = visitOwner(frame)
    fun visitPriv(frame: PrivFrame): R = visitOwner(frame)

    // Binary container frames: default to visitBinary.
    fun visitMcdi(frame: McdiFrame): R = visitBinary(frame)
    fun visitEtco(frame: EtcoFrame): R = visitBinary(frame)
    fun visitMllt(frame: MlltFrame): R = visitBinary(frame)
    fun visitSytc(frame: SytcFrame): R = visitBinary(frame)
    fun visitRva2(frame: Rva2Frame): R = visitBinary(frame)
    fun visitEqu2(frame: Equ2Frame): R = visitBinary(frame)
    fun visitRvrb(frame: RvrbFrame): R = visitBinary(frame)
    fun visitApic(frame: ApicFrame): R = visitBinary(frame)
    fun visitGeob(frame: GeobFrame): R = visitBinary(frame)
    fun visitPcnt(frame: PcntFrame): R = visitBinary(frame)
    fun visitPopm(frame: PopmFrame): R = visitBinary(frame)
    fun visitRbuf(frame: RbufFrame): R = visitBinary(frame)
    fun visitLink(frame: LinkFrame): R = visitBinary(frame)
    fun visitPoss(frame: PossFrame): R = visitBinary(frame)
    fun visitOwne(frame: OwneFrame): R = visitBinary(frame)
    fun visitComr(frame: ComrFrame): R = visitBinary(frame)
    fun visitSign(frame: SignFrame): R = visitBinary(frame)
    fun visitSeek(frame: SeekFrame): R = visitBinary(frame)
    fun visitAspi(frame: AspiFrame): R = visitBinary(frame)
    fun visitEqua(frame: EquaFrame): R = visitBinary(frame)
    fun visitRvad(frame: RvadFrame): R = visitBinary(frame)
    fun visitIpls(frame: IplsFrame): R = visitBinary(frame)
}
