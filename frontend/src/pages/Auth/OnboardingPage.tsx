import { type ChangeEvent, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { axiosInstance } from "../../api/axiosInstance";
import {
  Button,
  Chip,
  Icon,
  IconButton,
  Input,
  Switch,
  Textarea,
} from "../../components/ui";
import {
  themeCatalogService,
  type ThemeOption,
} from "../../features/themes/themeCatalog";

type UserType = "ACTIVE_RUNNER" | "RELAXED_TRAVELER" | "JEJU_RESIDENT";
type Distance = "UNDER_3KM" | "FROM_5_TO_10KM" | "OVER_10KM";
type Difficulty = "EASY" | "NORMAL" | "HARD";
type TermKey = "service" | "privacy" | "location" | "marketing";

type TermItem = {
  key: TermKey;
  label: string;
  sections: { title: string; paragraphs: string[] }[];
};

const TERM_ITEMS: TermItem[] = [
  {
    key: "service",
    label: "(필수) 서비스 이용약관 동의",
    sections: [
      {
        title: "서비스 목적",
        paragraphs: [
          "러닝올레는 제주 러닝 코스 탐색, 러닝 기록, 여행 정보와 커뮤니티 기능을 제공하는 공모전 시연 및 포트폴리오 목적의 서비스입니다.",
        ],
      },
      {
        title: "이용자의 책임",
        paragraphs: [
          "이용자는 정확한 정보를 입력하고 다른 이용자의 권리를 침해하거나 서비스 운영을 방해해서는 안 됩니다.",
          "코스와 안전시설 정보는 참고 자료이며, 실제 러닝 전 날씨·통제 상황·현장 안전을 직접 확인해야 합니다.",
        ],
      },
      {
        title: "게시물과 이용 제한",
        paragraphs: [
          "이용자가 작성한 코스, 게시물과 댓글의 권리는 작성자에게 있습니다. 서비스는 기능 제공과 화면 노출에 필요한 범위에서 이를 이용할 수 있습니다.",
          "타인의 권리를 침해하거나 안전한 이용을 방해하는 콘텐츠는 안내 후 숨김 또는 삭제될 수 있습니다.",
        ],
      },
      {
        title: "서비스 변경 및 종료",
        paragraphs: [
          "시연 서비스 특성상 기능이 변경되거나 운영이 종료될 수 있습니다. 중요한 변경은 서비스 화면을 통해 안내합니다.",
        ],
      },
    ],
  },
  {
    key: "privacy",
    label: "(필수) 개인정보 수집·이용 동의",
    sections: [
      {
        title: "수집 항목",
        paragraphs: [
          "카카오 계정 식별자, 닉네임, 프로필 사진과 자기소개, 사용자 유형, 선호 거리·난이도, 관심 테마, 알림 설정을 수집합니다. 프로필 사진과 자기소개는 선택 항목입니다.",
          "서비스 이용 중 작성한 코스, 러닝 기록, 게시물·댓글 및 서비스 이용 기록이 추가로 생성될 수 있습니다.",
        ],
      },
      {
        title: "이용 목적",
        paragraphs: [
          "회원 식별과 계정 관리, 맞춤 코스 추천, 러닝 기록 제공, 커뮤니티 운영, 문의 대응 및 서비스 개선에 이용합니다.",
        ],
      },
      {
        title: "보유 기간",
        paragraphs: [
          "회원 탈퇴 시 지체 없이 삭제하는 것을 원칙으로 합니다. 이용자가 직접 삭제한 콘텐츠도 서비스 운영상 필요한 처리 후 삭제합니다.",
          "관계 법령에 따라 보존할 의무가 있는 정보는 해당 기간 동안 별도로 보관할 수 있습니다.",
        ],
      },
      {
        title: "동의 거부",
        paragraphs: [
          "필수 정보 수집에 동의하지 않으면 회원 계정과 맞춤 서비스를 제공하기 어려워 가입을 완료할 수 없습니다.",
        ],
      },
    ],
  },
  {
    key: "location",
    label: "(필수) 개인위치정보 수집·이용 동의",
    sections: [
      {
        title: "이용하는 위치정보",
        paragraphs: [
          "현재 위치와 러닝 중 이동 경로를 이용해 주변 코스·장소 추천, 경로 안내와 러닝 기록 기능을 제공합니다.",
        ],
      },
      {
        title: "이용 및 보유 기간",
        paragraphs: [
          "현재 위치는 기능 제공에 필요한 동안 이용합니다. 이용자가 러닝 기록을 저장하면 이동 경로는 해당 기록을 삭제하거나 회원 탈퇴할 때까지 보관될 수 있습니다.",
        ],
      },
      {
        title: "이용자의 권리",
        paragraphs: [
          "위치 권한은 기기 설정에서 언제든 철회할 수 있고 저장된 러닝 기록을 삭제할 수 있습니다. 권한을 거부해도 위치 기능을 제외한 서비스는 이용할 수 있습니다.",
        ],
      },
      {
        title: "제3자 제공",
        paragraphs: [
          "개인위치정보를 이용자가 지정하지 않은 제3자에게 제공하지 않습니다. 제공이 필요한 경우 대상과 목적을 알리고 별도 동의를 받습니다.",
        ],
      },
    ],
  },
  {
    key: "marketing",
    label: "(선택) 마케팅 정보 수신 동의",
    sections: [
      {
        title: "수신 내용",
        paragraphs: [
          "러닝올레의 새로운 기능, 추천 코스, 행사와 이벤트 소식을 앱 알림으로 받을 수 있습니다.",
        ],
      },
      {
        title: "보유 기간 및 철회",
        paragraphs: [
          "동의일부터 회원 탈퇴 또는 수신 동의 철회 시까지 이용합니다. 알림 설정에서 언제든 수신을 중단할 수 있습니다.",
        ],
      },
      {
        title: "선택 동의 안내",
        paragraphs: [
          "동의하지 않아도 러닝올레의 기본 기능을 이용할 수 있습니다.",
        ],
      },
    ],
  },
];

const initialForm = {
  nickname: "",
  profileImageUrl: "",
  bio: "",
  userTypes: [] as UserType[],
  preferredDistance: "" as Distance | "",
  preferredDifficulty: "" as Difficulty | "",
  themeIds: [] as string[],
  terms: { service: false, privacy: false, location: false, marketing: false },
  notifications: {
    recommendedCourse: true,
    weather: true,
    meetupInvite: true,
    commentLike: false,
  },
};

function Progress({ step }: { step: number }) {
  return (
    <div className="progress-wrap">
      <span>{step} / 3 단계</span>
      <div className="progress-bars">
        {[1, 2, 3].map((item) => (
          <i key={item} className={item <= step ? "active" : ""} />
        ))}
      </div>
    </div>
  );
}

function Choice<T extends string>({
  value,
  selected,
  label,
  emoji,
  onClick,
}: {
  value: T;
  selected: boolean;
  label: string;
  emoji?: string;
  onClick: (value: T) => void;
}) {
  return (
    <Chip variant="choice" selected={selected} onClick={() => onClick(value)}>
      {emoji} {label}
    </Chip>
  );
}

export function OnboardingPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [form, setForm] = useState(initialForm);
  const [themes, setThemes] = useState<ThemeOption[]>([]);
  const [themeLoadError, setThemeLoadError] = useState(false);
  const [nicknameStatus, setNicknameStatus] = useState<
    "idle" | "checking" | "available" | "taken"
  >("idle");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    themeCatalogService
      .list()
      .then((items) => {
        setThemes(items);
        setThemeLoadError(false);
      })
      .catch(() => {
        setThemes([]);
        setThemeLoadError(true);
      });
  }, []);
  const [openTerm, setOpenTerm] = useState<TermKey | null>(null);

  useEffect(() => {
    if (form.nickname.trim().length < 2) {
      setNicknameStatus("idle");
      return;
    }

    setNicknameStatus("checking");
    const timer = window.setTimeout(async () => {
      try {
        const { data } = await axiosInstance.get(
          "/users/nickname-availability",
          { params: { nickname: form.nickname.trim() } },
        );
        setNicknameStatus(data.available ? "available" : "taken");
      } catch {
        setNicknameStatus("idle");
      }
    }, 350);

    return () => window.clearTimeout(timer);
  }, [form.nickname]);

  const stepValid = useMemo(() => {
    if (step === 1)
      return form.nickname.trim().length >= 2 && nicknameStatus !== "taken";
    if (step === 2)
      return (
        form.userTypes.length > 0 &&
        !!form.preferredDistance &&
        !!form.preferredDifficulty
      );
    return form.terms.service && form.terms.privacy && form.terms.location;
  }, [form, nicknameStatus, step]);

  const toggleUserType = (value: UserType) => {
    setForm((prev) => ({
      ...prev,
      userTypes: prev.userTypes.includes(value)
        ? prev.userTypes.filter((item) => item !== value)
        : [...prev.userTypes, value],
    }));
  };

  const toggleTheme = (themeId: string) => {
    setForm((prev) => ({
      ...prev,
      themeIds: prev.themeIds.includes(themeId)
        ? prev.themeIds.filter((id) => id !== themeId)
        : [...prev.themeIds, themeId],
    }));
  };

  const selectPhoto = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > 3 * 1024 * 1024) {
      setError("프로필 사진은 3MB 이하만 선택할 수 있습니다.");
      return;
    }

    const reader = new FileReader();
    reader.onload = () =>
      setForm((prev) => ({
        ...prev,
        profileImageUrl: String(reader.result),
      }));
    reader.readAsDataURL(file);
  };

  const goBack = async () => {
    if (step !== 1) {
      setStep((value) => value - 1);
      return;
    }

    try {
      await axiosInstance.post("/auth/logout");
    } finally {
      window.location.replace("/login");
    }
  };

  const next = async () => {
    setError("");
    if (!stepValid) return;
    if (step < 3) {
      setStep((value) => value + 1);
      window.scrollTo(0, 0);
      return;
    }

    setSubmitting(true);
    try {
      await axiosInstance.post("/users/me/onboarding", form);
      navigate("/", { replace: true });
    } catch (requestError: unknown) {
      const message = isApiError(requestError)
        ? requestError.response?.data?.message
        : undefined;
      setError(
        message ||
          "가입 정보를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const allTerms = Object.values(form.terms).every(Boolean);
  const setTerm = (key: TermKey, value: boolean) => {
    setForm((prev) => ({ ...prev, terms: { ...prev.terms, [key]: value } }));
  };
  const setNotification = (key: keyof typeof form.notifications) => {
    setForm((prev) => ({
      ...prev,
      notifications: { ...prev.notifications, [key]: !prev.notifications[key] },
    }));
  };

  return (
    <main className="onboarding-page">
      <header className="onboarding-header">
        <IconButton
          icon={<Icon name="arrowLeft" />}
          label="뒤로 가기"
          onClick={goBack}
        />
        <strong>회원가입</strong>
        <span />
      </header>
      <Progress step={step} />
      <section className="onboarding-content">
        {step === 1 && (
          <>
            <div className="account-card">
              <span className="kakao-account-icon">K</span>
              <div>
                <strong>카카오 계정 연결 완료</strong>
                <small>카카오 계정으로 안전하게 연결되었습니다.</small>
              </div>
            </div>
            <p className="eyebrow">프로필 설정</p>
            <h1>러닝올레에서 어떻게 불러드릴까요?</h1>
            <label className="photo-picker">
              <input type="file" accept="image/*" onChange={selectPhoto} />
              <span
                className="photo-preview"
                style={
                  form.profileImageUrl
                    ? { backgroundImage: `url(${form.profileImageUrl})` }
                    : undefined
                }
              >
                {!form.profileImageUrl && "O"}
              </span>
              <em>+</em>
              <b>사진 선택</b>
            </label>
            <Input
              label="닉네임"
              maxLength={100}
              value={form.nickname}
              onChange={(event) =>
                setForm({ ...form, nickname: event.target.value })
              }
              placeholder="러너제주"
              message={
                nicknameStatus === "checking"
                  ? "닉네임을 확인하고 있어요"
                  : nicknameStatus === "taken"
                    ? "이미 사용 중인 닉네임이에요"
                    : nicknameStatus === "available"
                      ? "✓ 사용 가능한 닉네임이에요"
                      : "2자 이상 입력해 주세요"
              }
              state={
                nicknameStatus === "taken"
                  ? "error"
                  : nicknameStatus === "available"
                    ? "success"
                    : "default"
              }
            />
            <Textarea
              label="자기소개 (선택)"
              count={`${form.bio.length} / 100자`}
              maxLength={100}
              value={form.bio}
              onChange={(event) =>
                setForm({ ...form, bio: event.target.value })
              }
              placeholder="제주의 오름과 바다를 사랑하는 러닝 여행자입니다."
            />
          </>
        )}

        {step === 2 && (
          <>
            <p className="eyebrow">러닝 취향</p>
            <h1>어떤 스타일로 달리시나요?</h1>
            <div className="choice-group">
              <h2>
                사용자 유형 <small>복수 선택</small>
              </h2>
              <div className="choices">
                <Choice
                  value="ACTIVE_RUNNER"
                  emoji="A"
                  label="활동적인 러너"
                  selected={form.userTypes.includes("ACTIVE_RUNNER")}
                  onClick={toggleUserType}
                />
                <Choice
                  value="RELAXED_TRAVELER"
                  emoji="T"
                  label="여유로운 여행자"
                  selected={form.userTypes.includes("RELAXED_TRAVELER")}
                  onClick={toggleUserType}
                />
                <Choice
                  value="JEJU_RESIDENT"
                  emoji="J"
                  label="제주 거주민"
                  selected={form.userTypes.includes("JEJU_RESIDENT")}
                  onClick={toggleUserType}
                />
              </div>
            </div>
            <div className="choice-group">
              <h2>선호 거리</h2>
              <div className="choices">
                <Choice
                  value="UNDER_3KM"
                  label="3km 이하"
                  selected={form.preferredDistance === "UNDER_3KM"}
                  onClick={(value) =>
                    setForm({ ...form, preferredDistance: value })
                  }
                />
                <Choice
                  value="FROM_5_TO_10KM"
                  label="5~10km"
                  selected={form.preferredDistance === "FROM_5_TO_10KM"}
                  onClick={(value) =>
                    setForm({ ...form, preferredDistance: value })
                  }
                />
                <Choice
                  value="OVER_10KM"
                  label="10km 이상"
                  selected={form.preferredDistance === "OVER_10KM"}
                  onClick={(value) =>
                    setForm({ ...form, preferredDistance: value })
                  }
                />
              </div>
            </div>
            <div className="choice-group">
              <h2>선호 난이도</h2>
              <div className="choices">
                <Choice
                  value="EASY"
                  label="쉬움"
                  selected={form.preferredDifficulty === "EASY"}
                  onClick={(value) =>
                    setForm({ ...form, preferredDifficulty: value })
                  }
                />
                <Choice
                  value="NORMAL"
                  label="보통"
                  selected={form.preferredDifficulty === "NORMAL"}
                  onClick={(value) =>
                    setForm({ ...form, preferredDifficulty: value })
                  }
                />
                <Choice
                  value="HARD"
                  label="어려움"
                  selected={form.preferredDifficulty === "HARD"}
                  onClick={(value) =>
                    setForm({ ...form, preferredDifficulty: value })
                  }
                />
              </div>
            </div>
            <div className="choice-group">
              <h2>
                관심 테마 <small>복수 선택</small>
              </h2>
              {themes.length > 0 ? (
                <div className="choices">
                  {themes.map((theme) => (
                    <Choice
                      key={theme.id}
                      value={theme.id}
                      label={theme.name}
                      selected={form.themeIds.includes(theme.id)}
                      onClick={toggleTheme}
                    />
                  ))}
                </div>
              ) : (
                <p className="excluded-note">
                  {themeLoadError
                    ? "테마 목록을 불러오지 못했어요. 나중에 프로필에서 설정할 수 있어요."
                    : "테마 목록을 불러오는 중이에요."}
                </p>
              )}
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <p className="eyebrow">마지막 단계</p>
            <h1>약관 동의와 알림 설정</h1>
            <p className="terms-demo-notice">
              공모전 시연 및 포트폴리오 운영 기준으로 작성된 약관 안내입니다.
            </p>
            <button
              type="button"
              className={`agree-all ${allTerms ? "checked" : ""}`}
              aria-pressed={allTerms}
              onClick={() => {
                const nextValue = !allTerms;
                setForm((prev) => ({
                  ...prev,
                  terms: {
                    service: nextValue,
                    privacy: nextValue,
                    location: nextValue,
                    marketing: nextValue,
                  },
                }));
              }}
            >
              <i aria-hidden="true">✓</i>
              <span>
                전체 동의 <small>선택 항목 포함</small>
              </span>
            </button>
            <div className="term-list">
              {TERM_ITEMS.map((term) => {
                const isOpen = openTerm === term.key;
                const panelId = `term-panel-${term.key}`;
                return (
                  <div
                    className={`term-item ${isOpen ? "open" : ""}`}
                    key={term.key}
                  >
                    <div className="term-row">
                      <button
                        type="button"
                        className="term-check"
                        aria-label={`${term.label} ${form.terms[term.key] ? "동의 취소" : "동의"}`}
                        aria-pressed={form.terms[term.key]}
                        onClick={() => setTerm(term.key, !form.terms[term.key])}
                      >
                        <i
                          className={form.terms[term.key] ? "checked" : ""}
                          aria-hidden="true"
                        >
                          ✓
                        </i>
                      </button>
                      <button
                        type="button"
                        className="term-toggle"
                        aria-expanded={isOpen}
                        aria-controls={panelId}
                        onClick={() => setOpenTerm(isOpen ? null : term.key)}
                      >
                        <span>{term.label}</span>
                        <b aria-hidden="true">›</b>
                      </button>
                    </div>
                    {isOpen ? (
                      <div
                        id={panelId}
                        className="term-detail"
                        role="region"
                        aria-label={`${term.label} 상세 내용`}
                      >
                        {term.sections.map((section) => (
                          <section key={section.title}>
                            <h3>{section.title}</h3>
                            {section.paragraphs.map((paragraph) => (
                              <p key={paragraph}>{paragraph}</p>
                            ))}
                          </section>
                        ))}
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
            <hr />
            <h2 className="notification-title">알림 설정</h2>
            <div className="notification-box">
              {(
                [
                  ["meetupInvite", "번개 참여·일정 알림"],
                  ["commentLike", "댓글·좋아요 알림"],
                ] as const
              ).map(([key, label]) => (
                <Switch
                  key={key}
                  label={label}
                  checked={form.notifications[key]}
                  onCheckedChange={() => setNotification(key)}
                />
              ))}
            </div>
          </>
        )}
        {error && <p className="form-error">{error}</p>}
      </section>
      <footer className="onboarding-footer">
        <Button
          variant="primary"
          size="lg"
          fullWidth
          loading={submitting}
          disabled={!stepValid}
          onClick={next}
        >
          {submitting
            ? "가입 정보를 저장하는 중…"
            : step === 3
              ? "러닝올레 시작하기  →"
              : "다음  →"}
        </Button>
      </footer>
    </main>
  );
}

function isApiError(
  error: unknown,
): error is { response?: { data?: { message?: string } } } {
  return typeof error === "object" && error !== null && "response" in error;
}
