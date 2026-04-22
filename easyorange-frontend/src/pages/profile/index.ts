import '../../styles/main.css';
import '../../styles/profile.css';
import { profileApi } from '../../api/index.js';
import { storage, toast, formatDate, formatRelativeTime, modalManager } from '../../utils/index.js';
import { navigation } from '../../app/navigation.js';
import type { UserInfo, UserStats, Gender } from '../../types/index.js';
import { BasePage } from '../BasePage.js';

type ActivityType = 'login' | 'logout' | 'publish' | 'buy' | 'sell' | 'favorite' | 'follow';

interface ActivityData {
  id?: number;
  activityType: string;
  title?: string;
  description?: string;
  timeAgo?: string;
}

interface UserPreferences {
  theme: 'light' | 'dark';
  notificationEnabled: number;
  emailNotification: number;
  messageSound: number;
  showOnlineStatus: number;
  showPhone: number;
  showEmail: number;
}

interface SecuritySettings {
  securityScore: number;
  twoFactorEnabled: number;
  loginNotification: number;
}

interface ProfileData {
  userInfo?: Partial<UserInfo>;
  stats?: UserStats;
}

interface ProfileState {
  user: Partial<UserInfo>;
  preferences: UserPreferences | null;
  security: SecuritySettings | null;
  activities: ActivityData[];
}

interface EditInfoData {
  username?: string;
  email?: string;
  phone?: string;
  realName?: string;
  studentId?: string;
  gender?: Gender;
}

interface ProfilePageElements {
  activityList: HTMLElement | null;
  profileName: HTMLElement | null;
  profileHandle: HTMLElement | null;
  statPublished: HTMLElement | null;
  statSold: HTMLElement | null;
  statFollowers: HTMLElement | null;
  metricRevenue: HTMLElement | null;
  metricTransactions: HTMLElement | null;
  metricFavorites: HTMLElement | null;
  metricFollowing: HTMLElement | null;
  myProductCount: HTMLElement | null;
  myOrderCount: HTMLElement | null;
  myFavoriteCount: HTMLElement | null;
  infoEmail: HTMLElement | null;
  infoPhone: HTMLElement | null;
  infoStudentId: HTMLElement | null;
  infoRealName: HTMLElement | null;
  infoJoinDate: HTMLElement | null;
  infoLastActive: HTMLElement | null;
  securityScoreValue: HTMLElement | null;
  scoreCircle: SVGCircleElement | null;
  twoFactorSwitch: HTMLInputElement | null;
  loginAlertSwitch: HTMLInputElement | null;
  notifMsgSwitch: HTMLInputElement | null;
  notifEmailSwitch: HTMLInputElement | null;
  notifSoundSwitch: HTMLInputElement | null;
  privacyOnlineSwitch: HTMLInputElement | null;
  privacyContactSwitch: HTMLInputElement | null;
  notificationBtn: HTMLElement | null;
  editProfileBtn: HTMLElement | null;
  shareProfileBtn: HTMLElement | null;
  changePasswordBtn: HTMLElement | null;
  modalOverlay: HTMLElement | null;
  submitPasswordBtn: HTMLElement | null;
  cancelPasswordBtn: HTMLElement | null;
  submitEditInfoBtn: HTMLElement | null;
  cancelEditInfoBtn: HTMLElement | null;
  exportDataBtn: HTMLElement | null;
  improveSecurityBtn: HTMLElement | null;
  passwordForm: HTMLFormElement | null;
  currentPassword: HTMLInputElement | null;
  newPassword: HTMLInputElement | null;
  confirmPassword: HTMLInputElement | null;
  editUsername: HTMLInputElement | null;
  editEmail: HTMLInputElement | null;
  editPhone: HTMLInputElement | null;
  editRealName: HTMLInputElement | null;
  editStudentId: HTMLInputElement | null;
  editGender: HTMLSelectElement | null;
}

class ProfilePage extends BasePage<ProfilePageElements> {
  private state: ProfileState = {
    user: {},
    preferences: null,
    security: null,
    activities: []
  };

  protected cacheElements(): void {
    this.elements = {
      activityList: this.querySelector('#activityList'),
      profileName: this.querySelector('#profileName'),
      profileHandle: this.querySelector('#profileHandle'),
      statPublished: this.querySelector('#statPublished'),
      statSold: this.querySelector('#statSold'),
      statFollowers: this.querySelector('#statFollowers'),
      metricRevenue: this.querySelector('#metricRevenue'),
      metricTransactions: this.querySelector('#metricTransactions'),
      metricFavorites: this.querySelector('#metricFavorites'),
      metricFollowing: this.querySelector('#metricFollowing'),
      myProductCount: this.querySelector('#myProductCount'),
      myOrderCount: this.querySelector('#myOrderCount'),
      myFavoriteCount: this.querySelector('#myFavoriteCount'),
      infoEmail: this.querySelector('#infoEmail'),
      infoPhone: this.querySelector('#infoPhone'),
      infoStudentId: this.querySelector('#infoStudentId'),
      infoRealName: this.querySelector('#infoRealName'),
      infoJoinDate: this.querySelector('#infoJoinDate'),
      infoLastActive: this.querySelector('#infoLastActive'),
      securityScoreValue: this.querySelector('#securityScoreValue'),
      scoreCircle: this.querySelector('#scoreCircle') as SVGCircleElement | null,
      twoFactorSwitch: this.querySelector('#twoFactorSwitch') as HTMLInputElement | null,
      loginAlertSwitch: this.querySelector('#loginAlertSwitch') as HTMLInputElement | null,
      notifMsgSwitch: this.querySelector('#notifMsgSwitch') as HTMLInputElement | null,
      notifEmailSwitch: this.querySelector('#notifEmailSwitch') as HTMLInputElement | null,
      notifSoundSwitch: this.querySelector('#notifSoundSwitch') as HTMLInputElement | null,
      privacyOnlineSwitch: this.querySelector('#privacyOnlineSwitch') as HTMLInputElement | null,
      privacyContactSwitch: this.querySelector('#privacyContactSwitch') as HTMLInputElement | null,
      notificationBtn: this.querySelector('.notification-btn'),
      editProfileBtn: this.querySelector('#editProfileBtn'),
      shareProfileBtn: this.querySelector('#shareProfileBtn'),
      changePasswordBtn: this.querySelector('#changePasswordBtn'),
      modalOverlay: this.querySelector('#modalOverlay'),
      submitPasswordBtn: this.querySelector('#submitPasswordBtn'),
      cancelPasswordBtn: this.querySelector('#cancelPasswordBtn'),
      submitEditInfoBtn: this.querySelector('#submitEditInfoBtn'),
      cancelEditInfoBtn: this.querySelector('#cancelEditInfoBtn'),
      exportDataBtn: this.querySelector('#exportDataBtn'),
      improveSecurityBtn: this.querySelector('#improveSecurityBtn'),
      passwordForm: this.querySelector('#passwordForm') as HTMLFormElement | null,
      currentPassword: this.querySelector('#currentPassword') as HTMLInputElement | null,
      newPassword: this.querySelector('#newPassword') as HTMLInputElement | null,
      confirmPassword: this.querySelector('#confirmPassword') as HTMLInputElement | null,
      editUsername: this.querySelector('#editUsername') as HTMLInputElement | null,
      editEmail: this.querySelector('#editEmail') as HTMLInputElement | null,
      editPhone: this.querySelector('#editPhone') as HTMLInputElement | null,
      editRealName: this.querySelector('#editRealName') as HTMLInputElement | null,
      editStudentId: this.querySelector('#editStudentId') as HTMLInputElement | null,
      editGender: this.querySelector('#editGender') as HTMLSelectElement | null,
    };
  }

  protected bindEvents(): void {
    const { elements: el } = this;

    this.onEvent(el.notificationBtn, 'click', () => {
      toast.info('消息功能开发中，敬请期待');
    });

    this.querySelectorAll<HTMLElement>('.nav-item[data-section]').forEach((item) => {
      this.onEvent(item, 'click', (e: Event) => {
        e.preventDefault();
        const section = item.dataset.section;
        if (!section) { return; }
        this.querySelectorAll<HTMLElement>('.nav-item[data-section]').forEach((n) => n.classList.remove('active'));
        item.classList.add('active');
        this.querySelectorAll<HTMLElement>('.content-section').forEach((s) => s.classList.toggle('active', s.id === `${section}-section`));
        history.pushState(null, '', `#${section}`);
      });
    });

    this.onEvent(el.editProfileBtn, 'click', (e: Event) => {
      e.preventDefault();
      this.openEditInfoModal();
    });

    this.onEvent(el.shareProfileBtn, 'click', () => { this.handleShareProfile(); });

    this.onEvent(el.changePasswordBtn, 'click', () => { this.openModal('passwordModal'); });

    this.onEvent(el.modalOverlay, 'click', () => { this.closeModals(); });

    this.querySelectorAll<HTMLElement>('.modal-close').forEach((btn) => {
      this.onEvent(btn, 'click', () => { this.closeModals(); });
    });

    this.onEvent(el.submitPasswordBtn, 'click', () => { this.handlePasswordSubmit(); });
    this.onEvent(el.cancelPasswordBtn, 'click', () => { this.closeModals(); });

    this.onEvent(el.submitEditInfoBtn, 'click', () => { this.handleEditInfoSubmit(); });
    this.onEvent(el.cancelEditInfoBtn, 'click', () => { this.closeModals(); });

    this.onEvent(el.exportDataBtn, 'click', () => { this.handleExportData(); });

    this.onEvent(el.improveSecurityBtn, 'click', () => {
      const securityNav = this.querySelectorAll<HTMLElement>('.nav-item[data-section="security"]')[0];
      if (securityNav) { securityNav.click(); }
    });

    this.querySelectorAll<HTMLElement>('.theme-option').forEach((opt) => {
      this.onEvent(opt, 'click', () => { this.selectTheme(opt.dataset.theme as 'light' | 'dark'); });
    });

    this.onEvent(el.twoFactorSwitch, 'change', (e: Event) => {
      this.updateSecuritySetting('twoFactorEnabled', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(el.loginAlertSwitch, 'change', (e: Event) => {
      this.updateSecuritySetting('loginNotification', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(el.notifMsgSwitch, 'change', (e: Event) => {
      this.updatePreference('notificationEnabled', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(el.notifEmailSwitch, 'change', (e: Event) => {
      this.updatePreference('emailNotification', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(el.notifSoundSwitch, 'change', (e: Event) => {
      this.updatePreference('messageSound', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(el.privacyOnlineSwitch, 'change', (e: Event) => {
      this.updatePreference('showOnlineStatus', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(el.privacyContactSwitch, 'change', (e: Event) => {
      this.updatePreference('showPhone', (e.target as HTMLInputElement).checked ? 1 : 0);
    });

    this.onEvent(document, 'keydown', (e: Event) => {
      if ((e as KeyboardEvent).key === 'Escape') { this.closeModals(); }
    });

    const hash = location.hash.slice(1);
    if (['overview', 'info', 'security', 'preferences'].includes(hash)) {
      const navItem = this.querySelector<HTMLElement>(`.nav-item[data-section="${hash}"]`);
      if (navItem) { navItem.click(); }
    }
  }

  protected async onInit(): Promise<void> {
    if (!navigation.requireAuth()) {
      return;
    }

    const localUser = storage.get<Partial<UserInfo>>('user');

    if (localUser && typeof localUser === 'object') {
      this.state.user = localUser;
    }

    this.updateAllUI();

    try {
      await this.loadAPIData();
      await this.loadActivities();
    } catch (e) {
      // 数据加载失败时静默处理，不影响页面渲染
    }

    this.animateCounters();
    this.checkFirstLogin();
  }

  private checkFirstLogin(): void {
    const urlParams = new URLSearchParams(window.location.search);
    const isFirstLoginParam = urlParams.get('firstLogin') === '1';
    const needCompleteProfile = storage.get<string>('needCompleteProfile');

    if (isFirstLoginParam || needCompleteProfile === 'true') {
      storage.remove('needCompleteProfile');
      history.replaceState(null, '', window.location.pathname);

      setTimeout(() => {
        toast.info('🎉 注册成功！请完善您的个人资料', 5000);
      }, 500);

      setTimeout(() => {
        const infoNav = this.querySelector<HTMLElement>('.nav-item[data-section="info"]');
        if (infoNav) {
          infoNav.click();
        }
      }, 800);
    }
  }

  private updateAllUI(): void {
    this.updateHeader();
    this.updateProfileCard(this.state.user);
    this.updateInfoSection(this.state.user);
    if (this.state.preferences) { this.updatePreferencesUI(); }
    if (this.state.security) { this.updateSecurityUI(); }
  }

  private updateHeader(): void {
    // 头像功能已移除
  }

  private async loadAPIData(): Promise<void> {
    try {
      const [profileRes, prefsRes, securityRes] = await Promise.allSettled([
        profileApi.getProfile(),
        profileApi.getPreferences(),
        profileApi.getSecurity()
      ]);

      if (profileRes.status === 'fulfilled' && profileRes.value?.data) {
        const profile = profileRes.value.data as ProfileData;
        if (profile.userInfo) {
          this.state.user = { ...this.state.user, ...profile.userInfo };
          this.updateProfileCard(profile.userInfo);
          this.updateInfoSection(profile.userInfo);
        }
        if (profile.stats) {
          this.updateMetrics(profile.stats);
        }
      }

      if (prefsRes.status === 'fulfilled' && prefsRes.value?.data) {
        this.state.preferences = prefsRes.value.data as unknown as UserPreferences;
        this.updatePreferencesUI();
      } else {
        this.state.preferences = this.getDefaultPreferences();
        this.updatePreferencesUI();
      }

      if (securityRes.status === 'fulfilled' && securityRes.value?.data) {
        this.state.security = securityRes.value.data as unknown as SecuritySettings;
        this.updateSecurityUI();
      } else {
        this.state.security = this.getDefaultSecurity();
        this.updateSecurityUI();
      }
    } catch (e) {
      this.state.preferences = this.getDefaultPreferences();
      this.state.security = this.getDefaultSecurity();
      this.updatePreferencesUI();
      this.updateSecurityUI();
    }
  }

  private async loadActivities(): Promise<void> {
    try {
      const res = await profileApi.getActivities(1, 5);
      if (res) {
        const data = res.data ?? [];
        this.state.activities = Array.isArray(data) ? data as unknown as ActivityData[] : [];
        this.renderActivities();
      }
    } catch (e) {
      // 活动数据加载失败时静默处理
    }
  }

  private renderActivities(): void {
    const { elements: el } = this;
    if (!el.activityList) { return; }

    if (!this.state.activities || this.state.activities.length === 0) {
      el.activityList.innerHTML = '<div class="activity-empty"><p>暂无最近活动</p></div>';
      return;
    }

    el.activityList.innerHTML = this.state.activities.map((a: ActivityData) => `
      <div class="activity-item">
        <div class="activity-icon ${this.getActivityIconClass(a.activityType as ActivityType)}">
          ${this.getActivityIcon(a.activityType as ActivityType)}
        </div>
        <div class="activity-content">
          <div class="activity-title">${a.title || '活动'}</div>
          <div class="activity-desc">${a.description || ''}</div>
        </div>
        <div class="activity-time">${a.timeAgo || ''}</div>
      </div>
    `).join('');
  }

  private getActivityIconClass(type: ActivityType): string {
    const classes: Record<ActivityType, string> = {
      login: 'green',
      logout: 'gray',
      publish: 'blue',
      buy: 'orange',
      sell: 'purple',
      favorite: 'pink',
      follow: 'cyan'
    };
    return classes[type] || 'default';
  }

  private getActivityIcon(type: ActivityType): string {
    const icons: Record<ActivityType, string> = {
      login: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>',
      logout: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>',
      publish: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>',
      buy: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>',
      sell: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>',
      favorite: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>',
      follow: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>'
    };
    return icons[type] || '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>';
  }

  private getDefaultPreferences(): UserPreferences {
    return {
      theme: 'light',
      notificationEnabled: 1,
      emailNotification: 1,
      messageSound: 0,
      showOnlineStatus: 1,
      showPhone: 0,
      showEmail: 0
    };
  }

  private getDefaultSecurity(): SecuritySettings {
    return { securityScore: 60, twoFactorEnabled: 0, loginNotification: 1 };
  }

  private updateProfileCard(user: Partial<UserInfo> | null): void {
    if (!user) { return; }
    const { elements: el } = this;
    const completeness = this.getProfileCompleteness(user);
    this.safe(el.profileName, (name) => { name.textContent = user.username || '用户'; });
    this.safe(el.profileHandle, (handle) => { handle.textContent = (user.username || 'user').toLowerCase(); });
    this.setText('#profileIntro', this.getProfileIntro(user, completeness));
    this.setText('#profileCompleteness', `${completeness}%`);
    this.setText('#profileLastSeen', user.lastLoginTime ? formatRelativeTime(user.lastLoginTime) : '首次启用');
    this.setText('#profileCampusTag', user.studentId ? '校园身份已绑定' : '校园身份待完善');
    const userStats = user as Partial<UserInfo> & { publishedCount?: number; soldCount?: number; followerCount?: number };
    this.safe(el.statPublished, (published) => { published.dataset.count = String(userStats.publishedCount || 12); });
    this.safe(el.statSold, (sold) => { sold.dataset.count = String(userStats.soldCount || 8); });
    this.safe(el.statFollowers, (followers) => { followers.dataset.count = String(userStats.followerCount || 45); });
  }

  private getProfileCompleteness(user: Partial<UserInfo>): number {
    const fields = [user.username, user.email, user.phone, user.studentId, user.realName];
    const completedCount = fields.filter((value) => typeof value === 'string' ? value.trim().length > 0 : Boolean(value)).length;
    return Math.round((completedCount / fields.length) * 100);
  }

  private getProfileIntro(user: Partial<UserInfo>, completeness: number): string {
    if (user.realName && user.studentId) {
      return `${user.realName} 的主页已具备校园身份信息，适合更安心地展示与交易。`;
    }

    if (completeness >= 80) {
      return '资料已经比较完整，继续保持清晰可信的校园交易形象。';
    }

    return '补齐联系方式和身份信息后，这里会成为更有说服力的交易名片。';
  }

  private updateMetrics(stats: UserStats | null): void {
    if (!stats) { return; }
    const { elements: el } = this;
    const statsExt = stats as unknown as { totalTransactionAmount?: number; transactionCount?: number; orderCount?: number };
    this.safe(el.metricRevenue, (revenue) => { revenue.textContent = `¥${(statsExt.totalTransactionAmount || 0).toFixed(2)}`; });
    this.safe(el.metricTransactions, (transactions) => { transactions.textContent = String(statsExt.transactionCount || stats.boughtCount || 0); });
    this.safe(el.metricFavorites, (favorites) => { favorites.textContent = String(stats.favoriteCount || 0); });
    this.safe(el.metricFollowing, (following) => { following.textContent = String(stats.followingCount || 0); });

    this.safe(el.myProductCount, (count) => { count.textContent = `${stats.productCount || 0} 件`; });
    this.safe(el.myOrderCount, (count) => { count.textContent = `${statsExt.orderCount || stats.boughtCount || 0} 笔`; });
    this.safe(el.myFavoriteCount, (count) => { count.textContent = `${stats.favoriteCount || 0} 件`; });
  }

  private updateInfoSection(user: Partial<UserInfo> | null): void {
    if (!user) { return; }
    this.setText('#infoEmail', user.email || '未设置');
    this.setText('#infoPhone', user.phone || '未设置');
    this.setText('#infoStudentId', user.studentId || '未设置');
    this.setText('#infoRealName', user.realName || '未设置');
    this.setText('#infoJoinDate', user.createTime ? formatDate(user.createTime, 'date') : '未知');
    this.setText('#infoLastActive', user.lastLoginTime ? formatDate(user.lastLoginTime, 'date') : '未知');
  }

  private setText(selector: string, value: string): void {
    const el = this.querySelector<HTMLElement>(selector);
    this.safe(el, (element) => { element.textContent = value; });
  }

  private updateSecurityUI(): void {
    if (!this.state.security) { return; }
    const { elements: el } = this;
    const sec = this.state.security;
    this.safe(el.securityScoreValue, (scoreEl) => { scoreEl.textContent = String(sec.securityScore || 60); });
    this.safe(el.scoreCircle, (circle) => { circle.style.strokeDashoffset = String(339.292 - ((sec.securityScore || 60) / 100) * 339.292); });
    this.safe(el.twoFactorSwitch, (twoFactor) => { twoFactor.checked = sec.twoFactorEnabled === 1; });
    this.safe(el.loginAlertSwitch, (loginAlert) => { loginAlert.checked = sec.loginNotification === 1; });
  }

  private updatePreferencesUI(): void {
    if (!this.state.preferences) { return; }
    const { elements: el } = this;
    const prefs = this.state.preferences;
    this.querySelectorAll<HTMLElement>('.theme-option').forEach((opt) =>
      opt.classList.toggle('active', opt.dataset.theme === prefs.theme)
    );
    this.safe(el.notifMsgSwitch, (notifMsg) => { notifMsg.checked = prefs.notificationEnabled === 1; });
    this.safe(el.notifEmailSwitch, (notifEmail) => { notifEmail.checked = prefs.emailNotification === 1; });
    this.safe(el.notifSoundSwitch, (notifSound) => { notifSound.checked = prefs.messageSound === 1; });
    this.safe(el.privacyOnlineSwitch, (privacyOnline) => { privacyOnline.checked = prefs.showOnlineStatus === 1; });
    this.safe(el.privacyContactSwitch, (privacyContact) => { privacyContact.checked = prefs.showPhone === 1; });
  }

  private animateCounters(): void {
    this.querySelectorAll<HTMLElement>('.stat-number[data-count]').forEach((el) => {
      const target = parseInt(el.dataset.count || '0') || 0;
      const start = performance.now();
      const tick = (now: number): void => {
        const p = Math.min((now - start) / 1000, 1);
        el.textContent = String(Math.floor(target * (1 - Math.pow(1 - p, 3))));
        if (p < 1) { requestAnimationFrame(tick); }
      };
      requestAnimationFrame(tick);
    });
  }

  private openModal(id: string): void {
    modalManager.open(id, { closeOnOverlayClick: true });
  }

  private openEditInfoModal(): void {
    const { elements: el } = this;
    this.safe(el.editUsername, (input) => { input.value = this.state.user.username || ''; });
    this.safe(el.editEmail, (input) => { input.value = this.state.user.email || ''; });
    this.safe(el.editPhone, (input) => { input.value = this.state.user.phone || ''; });
    this.safe(el.editRealName, (input) => { input.value = this.state.user.realName || ''; });
    this.safe(el.editStudentId, (input) => { input.value = this.state.user.studentId || ''; });
    this.safe(el.editGender, (select) => { select.value = this.state.user.gender ?? ''; });

    this.openModal('editInfoModal');
  }

  private closeModals(): void {
    modalManager.closeAll();
    this.safe(this.elements.passwordForm, (form) => { form.reset(); });
  }

  private async handleShareProfile(): Promise<void> {
    const url = `${window.location.origin}/user/${this.state.user.id || ''}`;
    try {
      await navigator.clipboard.writeText(url);
      toast.success('链接已复制到剪贴板');
    } catch {
      toast.info(`分享链接: ${url}`);
    }
  }

  private async handleExportData(): Promise<void> {
    try {
      toast.info('正在导出数据...');
      const res = await profileApi.exportData();
      if (res?.data) {
        const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `user-data-${new Date().toISOString().slice(0, 10)}. json`;
        a.click();
        URL.revokeObjectURL(url);
        toast.success('数据导出成功');
      }
    } catch (e) {
      const error = e as Error;
      toast.error(`导出失败: ${error.message || '未知错误'}`);
    }
  }

  private async handlePasswordSubmit(): Promise<void> {
    const { elements: el } = this;
    const current = el.currentPassword?.value;
    const newPwd = el.newPassword?.value;
    const confirm = el.confirmPassword?.value;

    if (!current || !newPwd || !confirm) { return toast.error('请填写所有字段'); }
    if (newPwd !== confirm) { return toast.error('两次密码不一致'); }
    if ( newPwd.length < 6) { return toast.error('密码至少6位'); }

    try {
      await profileApi.changePassword({ currentPassword: current, newPassword: newPwd, confirmPassword: confirm });
      toast.success('密码修改成功');
      this.closeModals();
    } catch (e) {
      const error = e as Error;
      toast.error(error.message || '修改失败');
    }
  }

  private async handleEditInfoSubmit(): Promise<void> {
    const { elements: el } = this;
    const genderValue = el.editGender?.value;
    const data: EditInfoData = {
      username: el.editUsername?.value || undefined,
      email: el.editEmail?.value || undefined,
      phone: el.editPhone?.value || undefined,
      realName: el.editRealName?.value || undefined,
      studentId: el.editStudentId?.value || undefined,
      gender: genderValue ? (genderValue as Gender) : undefined
    };

    Object.keys(data).forEach((key) => {
      if (data[key as keyof EditInfoData] === undefined) {
        delete data[key as keyof EditInfoData];
      }
    });

    if (Object.keys(data).length === 0) { return toast.error('请修改至少一项内容'); }

    try {
      const res = await profileApi.updateUserInfo(data);
      if (res?.data) {
        this.state.user = { ...this.state.user, ...res.data };
        storage.set('user', this.state.user);
        this.updateProfileCard(this.state.user);
        this.updateInfoSection(this.state.user);
        toast.success('资料已更新');
        this.closeModals();
      }
    } catch (e) {
      const error = e as Error;
      toast.error(error.message || '更新失败');
    }
  }

  private async selectTheme(theme: 'light' | 'dark'): Promise<void> {
    this.state.preferences = { ...this.state.preferences, theme } as UserPreferences;
    this.querySelectorAll<HTMLElement>('.theme-option').forEach((opt) => opt.classList.toggle('active', opt.dataset.theme === theme));
    document.body.classList.toggle('dark-theme', theme === 'dark');
    try { await profileApi.updatePreferences({ theme }); } catch { /* 静默失败，不影响主题切换 */ }
    toast.success('主题已更新');
  }

  private async updateSecuritySetting(field: keyof SecuritySettings, value: number): Promise<void> {
    this.state.security = { ...this.state.security, [field]: value } as SecuritySettings;
    try { await profileApi.updateSecurity({ [field]: value }); } catch { /* 静默失败，不影响本地状态 */ }
    toast.success('设置已保存');
  }

  private async updatePreference(field: keyof UserPreferences, value: number): Promise<void> {
    this.state.preferences = { ...this.state.preferences, [field]: value } as UserPreferences;
    try { await profileApi.updatePreferences({ [field]: value }); } catch { /* 静默失败，不影响本地状态 */ }
    toast.success('设置已保存');
  }
}

let profilePage: ProfilePage | null = null;
document.addEventListener('DOMContentLoaded', async (): Promise<void> => {
  profilePage = new ProfilePage();
  await profilePage.init();
});

export { ProfilePage };
export default ProfilePage;