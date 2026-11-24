import { type ReactNode, useState, useEffect } from "react";
import {
  ArrowUpRight,
  BriefcaseBusiness,
  Building2,
  ChevronRight,
  Columns3,
  FileText,
  FlaskConical,
  FolderOpen,
  GitFork,
  LogIn,
  LogOut,
  Menu,
  PanelLeft,
  X,
} from "lucide-react";
import { Avatar } from "./Board";
import { demo } from "./recruitment";

export const views = {
  funil: { title: "Funil de seleção", icon: Columns3 },
  "minhas-vagas": { title: "Minhas vagas", icon: FolderOpen },
  vagas: { title: "Vagas abertas", icon: BriefcaseBusiness },
  minhas: { title: "Minhas candidaturas", icon: FileText },
};
export type View = keyof typeof views;

export function Workspace({
  links,
  view,
  user,
  signOut,
  enter,
  children,
}: {
  links: View[];
  view: View;
  user: { nome: string; papel: string } | null;
  signOut: () => void;
  enter: () => void;
  children: ReactNode;
}) {
  const [mobile, setMobile] = useState(false);
  useEffect(() => {
    const closeMenu = () => setMobile(false);
    window.addEventListener("hashchange", closeMenu);
    return () => window.removeEventListener("hashchange", closeMenu);
  }, []);
  return (
    <div className="app">
      {mobile && (
        <button
          className="backdrop"
          aria-label="Fechar navegação"
          onClick={() => setMobile(false)}
        />
      )}
      <aside className={mobile ? "sidebar shown" : "sidebar"}>
        <div className="brand">
          <span className="brand-icon">
            <GitFork size={24} />
          </span>
          Trilha<span className="brand-dot">.</span>
        </div>
        <div className="workspace">
          <span className="work-icon">
            <Building2 size={18} />
          </span>
          <div>
            <strong>{demo ? "Estúdio Aurora" : "Processo seletivo"}</strong>
            <small>Espaço de trabalho</small>
          </div>
        </div>
        <div className="nav-label">NAVEGAÇÃO</div>
        <nav>
          {links.map((key) => {
            const Icon = views[key].icon;
            return (
              <a
                key={key}
                className={key === view ? "selected" : ""}
                href={"#" + key}
                aria-current={key === view ? "page" : undefined}
                onClick={() => setMobile(false)}
              >
                <Icon size={18} />
                {views[key].title}
              </a>
            );
          })}
        </nav>
        <div className="side-bottom">
          {demo && (
            <div className="demo-note">
              <FlaskConical size={17} />
              <div>
                <b>Modo demonstração</b>
                <small>Dados fictícios nesta sessão</small>
                <a
                  href="https://github.com/kaua-chavesvk/processo-seletivo"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Código no GitHub <ArrowUpRight size={13} aria-hidden="true" />
                </a>
              </div>
            </div>
          )}
          <div className="identity">
            {user ? (
              <>
                <Avatar name={user.nome} />
                <div>
                  <b>{user.nome}</b>
                  <small>
                    {(
                      {
                        CANDIDATO: "Candidato",
                        RECRUTADOR: "Recrutador",
                        ADMIN: "Administrador",
                      } as Record<string, string>
                    )[user.papel] ?? user.papel}
                  </small>
                </div>
                {!demo && (
                  <button className="icon" onClick={signOut} aria-label="Sair">
                    <LogOut size={17} />
                  </button>
                )}
              </>
            ) : (
              <button className="secondary" onClick={enter}>
                <LogIn size={16} /> Entrar
              </button>
            )}
          </div>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <button
            className="mobile-menu icon"
            onClick={() => setMobile(!mobile)}
            aria-label="Abrir navegação"
          >
            {mobile ? <X /> : <Menu />}
          </button>
          <PanelLeft className="desktop-icon" size={17} />
          <span className="divider" />
          <span>Workspace</span>
          <ChevronRight size={13} />
          <b>{views[view].title}</b>
          {demo && (
            <span className="demo-chip">
              <i />
              Demonstração
            </span>
          )}
        </header>
        <main>{children}</main>
      </div>
    </div>
  );
}
